package com.istudy.content;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.istudy.identity.*;
import com.istudy.resources.ObjectStorage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/content")
public class QuestionBankController {

  private final JdbcTemplate db;
  private final CurrentStudent current;
  private final ObjectMapper json;
  private final ObjectStorage storage;
  private final QuestionBankValidator validator;
  private final com.istudy.catalogue.CatalogueService catalogue;

  public QuestionBankController(
    JdbcTemplate db,
    CurrentStudent current,
    ObjectMapper json,
    ObjectStorage storage,
    QuestionBankValidator validator,
    com.istudy.catalogue.CatalogueService catalogue
  ) {
    this.db = db;
    this.current = current;
    this.json = json;
    this.storage = storage;
    this.validator = validator;
    this.catalogue = catalogue;
  }

  private UUID admin() {
    UUID id = current.id();
    if (
      !Boolean.TRUE.equals(
        db.queryForObject(
          "select content_admin from app_users where user_id=?",
          Boolean.class,
          id
        )
      )
    ) throw new ResponseStatusException(
      HttpStatus.FORBIDDEN,
      "Content administration permission is required"
    );
    return id;
  }

  public record Bank(
    @NotNull UUID offeringId,
    @NotNull UUID documentId,
    @NotNull JsonNode manifest
  ) {}

  public record Review(boolean reviewConfirmed) {}

  public record IssueReview(@NotBlank String reason, boolean reviewed) {}

  @GetMapping("/question-issues")
  public List<Map<String, Object>> questionIssues() {
    admin();
    return db.query(
      "select i.session_question_id,i.reason,r.revision_number,r.content_snapshot,c.title as chapter_name,sub.name as subject_name from question_issue_reports i join session_questions sq on sq.session_question_id=i.session_question_id join question_revisions r on r.revision_id=sq.revision_id join practice_sessions ps on ps.session_id=sq.session_id join subject_offerings o on o.offering_id=ps.offering_id join subjects sub on sub.subject_id=o.subject_id left join question_placements p on p.placement_id=sq.placement_id left join sections sec on sec.section_id=p.section_id left join chapters c on c.chapter_id=sec.chapter_id where not i.reviewed order by i.created_at limit 200",
      (rs, n) -> {
        var issue = new LinkedHashMap<String, Object>();
        issue.put("sessionQuestionId", rs.getObject("session_question_id"));
        issue.put("reason", rs.getString("reason"));
        issue.put("revisionNumber", rs.getInt("revision_number"));
        issue.put("chapterName", rs.getString("chapter_name"));
        issue.put("subjectName", rs.getString("subject_name"));
        try {
          issue.put(
            "question",
            json.readTree(rs.getString("content_snapshot"))
          );
        } catch (java.io.IOException e) {
          throw new IllegalStateException("Invalid question revision", e);
        }
        return issue;
      }
    );
  }

  @PutMapping("/question-issues/{id}")
  public void reviewIssue(
    @PathVariable UUID id,
    @Valid @RequestBody IssueReview review
  ) {
    admin();
    if (
      db.update(
        "update question_issue_reports set reviewed=? where session_question_id=? and reason=?",
        review.reviewed(),
        id,
        review.reason()
      ) ==
      0
    ) throw new ResponseStatusException(
      HttpStatus.NOT_FOUND,
      "Report not found"
    );
  }

  @GetMapping("/question-sources")
  public List<Map<String, Object>> books() {
    admin();
    return db.queryForList(
      """
      select d.document_id as "documentId",d.title,d.asset_id as "assetId",d.licence_status as "licenceStatus",
        d.created_at as "createdAt",coalesce(d.bank_offering_id,history.offering_id) as "offeringId",
        case when d.document_type='TEXTBOOK' then 'TEXTBOOK' else 'ORIGINAL' end as "sourceType"
      from content_documents d
      join media_assets a on a.asset_id=d.asset_id
      left join (
        select document_id,cast(min(cast(offering_id as varchar(36))) as uuid) as offering_id
        from question_bank_imports group by document_id having count(distinct offering_id)=1
      ) history on history.document_id=d.document_id
      where d.document_type='TEXTBOOK' or (d.document_type='OTHER' and a.content_type='application/json')
      order by d.created_at desc
      """
    );
  }

  @PostMapping("/question-sources")
  @Transactional
  public Map<String, Object> book(
    @RequestParam MultipartFile file,
    @RequestParam String title,
    @RequestParam boolean licenceConfirmed,
    @RequestParam(defaultValue = "TEXTBOOK") String kind
  ) throws java.io.IOException {
    UUID user = admin();
    if (
      !licenceConfirmed || title.isBlank() || title.length() > 300
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Provide a title and confirm permission to use this source"
    );
    boolean original = "ORIGINAL".equals(kind);
    if (
      !original && !"TEXTBOOK".equals(kind)
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Choose TEXTBOOK or ORIGINAL source"
    );
    long max = original ? 5_000_000 : 50_000_000;
    if (
      file.isEmpty() || file.getSize() > max
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      original
        ? "Choose original question JSON smaller than 5 MB"
        : "Choose a PDF smaller than 50 MB"
    );
    byte[] bytes = file.getBytes();
    if (original) {
      JsonNode source;
      try {
        source = json.readTree(bytes);
      } catch (java.io.IOException e) {
        throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Choose valid original question JSON"
        );
      }
      if (
        source == null ||
        !source.isObject() ||
        (!source.path("questions").isArray() &&
          !source.path("chapters").isArray())
      ) throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Original source must contain questions or chapters"
      );
    } else if (
      bytes.length < 5 ||
      !new String(
        bytes,
        0,
        5,
        java.nio.charset.StandardCharsets.US_ASCII
      ).equals("%PDF-")
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Choose a valid PDF textbook"
    );
    UUID asset = UUID.randomUUID(), doc = UUID.randomUUID();
    String key = "question-sources/" + doc + (original ? ".json" : ".pdf");
    storage.write(key, new java.io.ByteArrayInputStream(bytes));
    db.update(
      "insert into media_assets(asset_id,storage_provider,bucket_name,object_key,content_type,byte_size,sha256,scan_status,owner_user_id,licence_note) values(?,'LOCAL','istudy-local',?,?,?,?,'PENDING',?,'Use authorised by uploading content administrator')",
      asset,
      key,
      original ? "application/json" : "application/pdf",
      bytes.length,
      digest(bytes),
      user
    );
    db.update(
      "insert into content_documents(document_id,asset_id,document_type,title,licence_status) values(?,?,?,?,'PERMITTED')",
      doc,
      asset,
      original ? "OTHER" : "TEXTBOOK",
      title
    );
    return Map.of("documentId", doc, "assetId", asset);
  }

  private String digest(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @GetMapping("/question-bank-objectives")
  public List<Map<String, Object>> objectives(@RequestParam UUID offeringId) {
    admin();
    return db.queryForList(
      """
      select o.section_id as "sectionId",o.code,o.title,o.required
      from learning_objectives o
      join sections s on s.section_id=o.section_id
      join chapters c on c.chapter_id=s.chapter_id
      where c.offering_id=? and c.status='ACTIVE' and s.status='ACTIVE'
      order by c.sort_order,s.sort_order,o.code
      """,
      offeringId
    );
  }

  private boolean permitted(Bank bank) {
    if (
      !Boolean.TRUE.equals(
        db.queryForObject(
          "select exists(select 1 from content_documents where document_id=? and licence_status='PERMITTED' and (document_type='TEXTBOOK' or (document_type='OTHER' and exists(select 1 from media_assets a where a.asset_id=content_documents.asset_id and a.content_type='application/json'))))",
          Boolean.class,
          bank.documentId()
        )
      )
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Choose an authorised textbook or original question source"
    );
    String actual = db.queryForObject(
      "select case when document_type='TEXTBOOK' then 'TEXTBOOK' else 'ORIGINAL' end from content_documents where document_id=?",
      String.class,
      bank.documentId()
    );
    if (
      !actual.equals(bank.manifest().path("sourceType").asText("TEXTBOOK"))
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "The bank sourceType must match the selected source"
    );
    var tracks = db.queryForList(
      "select track_code from subject_offerings where offering_id=? and status='PUBLISHED'",
      String.class, bank.offeringId()
    );
    if (tracks.isEmpty()) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Choose a published subject offering"
    );
    boolean levelled = Set.of("ORDINARY", "HIGHER").contains(tracks.get(0));
    UUID sourceOffering = db.queryForObject(
      "select bank_offering_id from content_documents where document_id=?",
      UUID.class, bank.documentId()
    );
    if (
      (sourceOffering != null && !sourceOffering.equals(bank.offeringId())) ||
      (levelled && Boolean.TRUE.equals(db.queryForObject(
        """
        select exists(
          select 1 from question_bank_imports b
          join content_documents d on d.document_id=b.document_id
          join media_assets a on a.asset_id=d.asset_id
          join content_documents incoming on incoming.document_id=?
          join media_assets asset on asset.asset_id=incoming.asset_id
          where b.offering_id<>? and (b.document_id=incoming.document_id or
            (incoming.document_type='TEXTBOOK' and d.document_type='TEXTBOOK' and a.sha256=asset.sha256))
        )
        """,
        Boolean.class, bank.documentId(), bank.offeringId()
      )))
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST, "Choose a question source for this course level"
    );
    if (levelled && !bank.manifest().path("formatVersion").asText().equals("2.0"))
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Use the format 2.0 template for this course level"
      );
    return levelled;
  }

  @PostMapping("/question-banks/validate")
  public Map<String, Object> validate(@Valid @RequestBody Bank bank) {
    admin();
    permitted(bank);
    return validator.validate(bank.offeringId(), bank.manifest());
  }

  @PostMapping("/question-banks")
  @Transactional
  public Map<String, Object> upload(@Valid @RequestBody Bank bank) {
    UUID user = admin();
    db.queryForList(
      """
      select d.document_id from content_documents d
      join media_assets a on a.asset_id=d.asset_id
      where d.document_id=? or (
        d.document_type='TEXTBOOK' and a.sha256=(
          select asset.sha256 from content_documents incoming join media_assets asset on asset.asset_id=incoming.asset_id
          where incoming.document_id=? and incoming.document_type='TEXTBOOK'
        )
      ) order by d.document_id for update
      """,
      UUID.class, bank.documentId(), bank.documentId()
    );
    boolean levelled = permitted(bank);
    var result = validator.validate(bank.offeringId(), bank.manifest());
    if (
      !Boolean.TRUE.equals(result.get("valid"))
    ) throw new ResponseStatusException(
      HttpStatus.UNPROCESSABLE_ENTITY,
      "Fix the validation issues before uploading this bank"
    );
    if (levelled) db.update(
      "update content_documents set bank_offering_id=? where document_id=? and bank_offering_id is null",
      bank.offeringId(), bank.documentId()
    );
    String hash = LocalAuthService.hash(bank.manifest().toString());
    int set = bank.manifest().path("questionSet").asInt();
    var existing = db.queryForList(
      "select import_id from question_bank_imports where offering_id=? and document_id=? and question_set=? and content_hash=?",
      UUID.class,
      bank.offeringId(),
      bank.documentId(),
      set,
      hash
    );
    UUID id;
    if (!existing.isEmpty()) id = existing.get(0);
    else {
      id = UUID.randomUUID();
      db.update(
        "insert into question_bank_imports(import_id,offering_id,document_id,question_set,manifest,content_hash,created_by) values(?,?,?,?,?,?,?)",
        id,
        bank.offeringId(),
        bank.documentId(),
        set,
        bank.manifest().toString(),
        hash,
        user
      );
    }
    return Map.of("importId", id, "validation", result);
  }

  @GetMapping("/question-banks")
  public List<Map<String, Object>> banks() {
    admin();
    var rows = db.queryForList(
      "select b.import_id as \"importId\",b.offering_id as \"offeringId\",b.question_set as \"questionSet\",b.status,b.created_at as \"createdAt\",d.title as \"sourceTitle\",b.document_id,b.manifest from question_bank_imports b join content_documents d on d.document_id=b.document_id order by b.created_at desc"
    );
    var latest = new ArrayList<Map<String, Object>>();
    var publishedScopes = new HashSet<String>();
    for (var row : rows) {
      try {
        var manifest = json.readTree(row.remove("manifest").toString());
        var codes = new TreeSet<String>();
        var titles = new ArrayList<String>();
        int count = 0;
        for (var chapter : manifest.path("chapters")) {
          codes.add(chapter.path("code").asText());
          titles.add(chapter.path("title").asText());
          for (var topic : chapter.path("topics")) count +=
          topic.path("questions").size();
        }
        String scope =
          row.get("offeringId") +
          ":" +
          row.remove("document_id") +
          ":" +
          row.get("questionSet") +
          ":" +
          manifest.path("coverageScope").asText() +
          ":" +
          codes;
        if (
          row.get("status").equals("PUBLISHED") && !publishedScopes.add(scope)
        ) continue;
        latest.add(row);
        row.put("chapterTitles", titles);
        row.put("questionCount", count);
      } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
        throw new IllegalStateException("Invalid stored question bank", e);
      }
    }
    return latest;
  }

  @GetMapping("/question-banks/{id}")
  public Map<String, Object> bank(@PathVariable UUID id) throws Exception {
    admin();
    var rows = db.queryForList(
      "select * from question_bank_imports where import_id=?",
      id
    );
    if (rows.isEmpty()) throw new ResponseStatusException(
      HttpStatus.NOT_FOUND,
      "Question bank not found"
    );
    var row = rows.get(0);
    JsonNode manifest = json.readTree(row.get("manifest").toString());
    return Map.of(
      "importId",
      id,
      "status",
      row.get("status"),
      "offeringId",
      row.get("offering_id"),
      "documentId",
      row.get("document_id"),
      "manifest",
      manifest,
      "validation",
      validator.validate((UUID) row.get("offering_id"), manifest)
    );
  }

  @PostMapping("/question-banks/{id}/publish")
  @Transactional
  public Map<String, Object> publish(
    @PathVariable UUID id,
    @RequestBody Review review
  ) throws Exception {
    UUID reviewer = admin();
    if (!review.reviewConfirmed()) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Confirm that answers, source pages and topic coverage have been reviewed"
    );
    var rows = db.queryForList(
      "select * from question_bank_imports where import_id=? for update",
      id
    );
    if (rows.isEmpty()) throw new ResponseStatusException(
      HttpStatus.NOT_FOUND,
      "Question bank not found"
    );
    var bank = rows.get(0);
    if (bank.get("status").equals("PUBLISHED")) return Map.of(
      "importId",
      id,
      "status",
      "PUBLISHED"
    );
    UUID offering = (UUID) bank.get("offering_id"), document = (UUID) bank.get(
      "document_id"
    );
    db.queryForObject(
      "select document_id from content_documents where document_id=? for update",
      UUID.class,
      document
    );
    boolean starterReplacement = db.queryForObject(
      "select is_starter from subject_offerings where offering_id=? for update",
      Boolean.class,
      offering
    );
    JsonNode manifest = json.readTree(bank.get("manifest").toString());
    permitted(new Bank(offering, document, manifest));
    var validation = validator.validate(offering, manifest);
    if (
      !Boolean.TRUE.equals(validation.get("valid"))
    ) throw new ResponseStatusException(
      HttpStatus.CONFLICT,
      "The catalogue changed; validate this bank again before publishing"
    );
    int set = ((Number) bank.get("question_set")).intValue();
    boolean planned = manifest.path("formatVersion").asText().equals("2.0");
    var published = new HashMap<String, UUID>();
    var reusable = new HashMap<JsonNode, UUID>();
    for (var existing : db.queryForList(
      """
      select q.question_id,r.content_snapshot
      from questions q
      join question_revisions r on r.question_id=q.question_id and r.revision_number=q.current_revision
      where q.approval_status='PUBLISHED' and q.visibility='GLOBAL' and exists (
        select 1 from question_placements p
        join sections s on s.section_id=p.section_id join chapters c on c.chapter_id=s.chapter_id
        where p.question_id=q.question_id and p.active and c.offering_id=?
      )
      """,
      offering
    )) {
      ObjectNode snapshot = (ObjectNode) json.readTree(
        existing.get("content_snapshot").toString()
      );
      reusable.put(reuseKey(snapshot), (UUID) existing.get("question_id"));
    }
    var chapters = new HashSet<UUID>();
    for (var chapter : manifest.path("chapters")) {
      UUID chapterId = catalogue.ensureChapter(offering, chapter);
      chapters.add(chapterId);
      for (var topic : chapter.path("topics")) {
        UUID section = catalogue.ensureSection(chapterId, topic);
        var objectives = planned
          ? prepareObjectives(section, topic, set)
          : Map.<String, UUID>of();
        for (var q : topic.path("questions")) {
          String external = q.path("externalId").asText();
          UUID question = published.get(external);
          if (question == null) {
            question = publishQuestion(
              document,
              set,
              external,
              validator.snapshot(q),
              q.hasNonNull("sourcePage") ? q.path("sourcePage").asInt() : null,
              reviewer,
              reusable
            );
            published.put(external, question);
          }
          placeQuestion(
            question, section, set, validator.snapshot(q).path("difficulty").asText(),
            objectives.get(q.path("objectiveCode").asText()), q.path("familyCode").asText(null)
          );
        }
      }
    }
    if (set == 1 && !published.isEmpty()) {
      if (starterReplacement) {
        var starterChapters = db.queryForList(
          "select distinct c.chapter_id from chapters c join sections s on s.chapter_id=c.chapter_id join question_placements p on p.section_id=s.section_id where c.offering_id=? and p.is_starter",
          UUID.class,
          offering
        );
        for (UUID old : starterChapters) if (!chapters.contains(old)) db.update(
          "update chapters set status='INACTIVE' where chapter_id=?",
          old
        );
        db.update(
          "update sections set status='INACTIVE' where chapter_id in (select chapter_id from chapters where offering_id=?) and exists(select 1 from question_placements p where p.section_id=sections.section_id and p.is_starter) and not exists(select 1 from question_placements p where p.section_id=sections.section_id and p.active and not p.is_starter)",
          offering
        );
      }
      db.update(
        "update question_placements set active=false where is_starter and section_id in (select s.section_id from sections s join chapters c on c.chapter_id=s.chapter_id where c.offering_id=?)",
        offering
      );
      db.update(
        "update subject_offerings set is_starter=false where offering_id=?",
        offering
      );
      db.update(
        "delete from study_notes where offering_id=? and is_starter",
        offering
      );
    }
    db.update(
      "update question_bank_imports set status='PUBLISHED',reviewed_by=?,published_at=current_timestamp where import_id=?",
      reviewer,
      id
    );
    if (set == 2) for (UUID chapter : chapters) db.update(
      "update content_jobs set status='COMPLETED',error_code=null,completed_at=current_timestamp where chapter_id=? and operation='GENERATE_QUESTIONS' and status in ('QUEUED','RUNNING','REVIEW')",
      chapter
    );
    return Map.of(
      "importId",
      id,
      "status",
      "PUBLISHED",
      "uniqueQuestions",
      published.size(),
      "questionSet",
      set
    );
  }

  private Map<String, UUID> prepareObjectives(UUID section, JsonNode topic, int set) {
    var ids = new HashMap<String, UUID>();
    for (var objective : topic.path("objectives")) {
      String code = objective.path("code").asText();
      var previous = db.queryForList(
        "select objective_id from learning_objectives where section_id=? and code=?",
        UUID.class, section, code
      );
      if (set == 2) {
        ids.put(code, previous.get(0));
        continue;
      }
      String title = objective.path("title").asText();
      boolean required = objective.path("required").asBoolean();
      if (previous.isEmpty()) {
        UUID id = UUID.randomUUID();
        db.update(
          "insert into learning_objectives(objective_id,section_id,code,title,required) values(?,?,?,?,?)",
          id, section, code, title, required
        );
        ids.put(code, id);
      } else {
        UUID id = previous.get(0);
        db.update("update learning_objectives set title=?,required=? where objective_id=?", title, required, id);
        ids.put(code, id);
      }
    }
    return ids;
  }

  private void placeQuestion(UUID question, UUID section, int set, String difficulty, UUID objective, String family) {
    int updated = objective == null
      ? db.update(
        "update question_placements set difficulty_override=? where question_id=? and section_id=? and question_set=? and usage_type='BOTH'",
        difficulty, question, section, set
      )
      : db.update(
        "update question_placements set difficulty_override=?,objective_id=?,family_code=? where question_id=? and section_id=? and question_set=? and usage_type='BOTH'",
        difficulty, objective, family, question, section, set
      );
    if (updated == 0) db.update(
      "insert into question_placements(question_id,section_id,usage_type,question_set,difficulty_override,objective_id,family_code) values(?,?,'BOTH',?,?,?,?)",
      question, section, set, difficulty, objective, objective == null ? null : family
    );
  }

  private UUID publishQuestion(
    UUID document,
    int set,
    String external,
    com.fasterxml.jackson.databind.node.ObjectNode snapshot,
    Integer page,
    UUID reviewer,
    Map<JsonNode, UUID> reusable
  ) {
    String hash = LocalAuthService.hash(snapshot.toString());
    var keys = db.queryForList(
      "select question_id,content_hash from external_question_keys where document_id=? and question_set=? and external_id=?",
      document,
      set,
      external
    );
    UUID question = keys.isEmpty()
      ? UUID.randomUUID()
      : (UUID) keys.get(0).get("question_id");
    if (
      !keys.isEmpty() && keys.get(0).get("content_hash").equals(hash)
    ) return question;
    if (keys.isEmpty()) {
      UUID reused = reusable.get(reuseKey(snapshot));
      if (reused != null) {
        db.update(
          "insert into external_question_keys values(?,?,?,?,?)",
          document,
          set,
          external,
          reused,
          hash
        );
        db.update(
          "insert into question_sources(question_id,document_id,page_number,source_note) values(?,?,?,?)",
          reused,
          document,
          page,
          "Manual bank: " + external + "; Set " + set
        );
        return reused;
      }
    }
    if (keys.isEmpty()) db.update(
      "insert into questions(question_id,question_text,difficulty_level,approval_status,creator_id) values(?,?,?,'DRAFT',?)",
      question,
      snapshot.path("prompt").asText(),
      snapshot.path("difficulty").asText(),
      reviewer
    );
    int revision = db.queryForObject(
      "select current_revision from questions where question_id=? for update",
      Integer.class,
      question
    );
    if (!keys.isEmpty()) revision++;
    UUID revisionId = UUID.randomUUID();
    String type = snapshot.path("type").asText();
    db.update(
      "insert into question_revisions(revision_id,question_id,revision_number,content_snapshot,reviewed_by) values(?,?,?,?,?)",
      revisionId,
      question,
      revision,
      snapshot.toString(),
      reviewer
    );
    db.update(
      "update questions set question_text=?,correct_answer=?,correct_answers=?,question_image_asset_id=?,question_type=?,explanation=?,difficulty_level=?,approval_status='PUBLISHED',current_revision=?,updated_at=current_timestamp where question_id=?",
      snapshot.path("prompt").asText(),
      snapshot.path("correctAnswer").asText(null),
      snapshot.has("correctAnswers")
        ? snapshot.get("correctAnswers").toString()
        : null,
      snapshot.hasNonNull("imageAssetId")
        ? UUID.fromString(snapshot.get("imageAssetId").asText())
        : null,
      type.equals("single")
        ? "SINGLE_CHOICE"
        : type.equals("multiple")
          ? "MULTIPLE_CHOICE"
          : type.equals("worked") ? "WORKED_EXAMPLE" : "TEXT",
      snapshot.path("explanation").asText(),
      snapshot.path("difficulty").asText(),
      revision,
      question
    );
    if (keys.isEmpty()) db.update(
      "insert into external_question_keys values(?,?,?,?,?)",
      document,
      set,
      external,
      question,
      hash
    );
    else db.update(
      "update external_question_keys set content_hash=? where document_id=? and question_set=? and external_id=?",
      hash,
      document,
      set,
      external
    );
    db.update(
      "insert into question_reviews(question_id,revision_id,reviewer_id,decision) values(?,?,?,'APPROVED')",
      question,
      revisionId,
      reviewer
    );
    db.update(
      "insert into question_sources(question_id,document_id,page_number,source_note) values(?,?,?,?)",
      question,
      document,
      page,
      "Manual bank: " + external + "; Set " + set
    );
    UUID saved = question;
    reusable.entrySet().removeIf(entry -> entry.getValue().equals(saved));
    reusable.put(reuseKey(snapshot), question);
    return question;
  }

  private ObjectNode reuseKey(ObjectNode snapshot) {
    var body = snapshot.deepCopy();
    body.remove(List.of("difficulty", "examLevel"));
    return body;
  }
}
