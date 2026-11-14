package com.istudy.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.istudy.identity.CurrentStudent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/content/inventory")
public class QuestionInventoryController {

  private static final String PLACEMENTS = """
    from question_placements p
    join questions q on q.question_id=p.question_id
    join sections s on s.section_id=p.section_id
    join chapters c on c.chapter_id=s.chapter_id
    join subject_offerings o on o.offering_id=c.offering_id
    """;

  private static final String ACTIVE = """
    where p.active and q.approval_status='PUBLISHED' and q.visibility='GLOBAL'
    and s.status='ACTIVE' and c.status='ACTIVE' and o.status='PUBLISHED'
    """;

  private static final String COURSES = """
    select o.offering_id as "offeringId",su.name as "subjectName",cu.name as "curriculumName",
      cu.country_code as "countryCode",l.name as "levelName",o.track_code as "trackCode",
      o.syllabus_version as "syllabusVersion"
    from subject_offerings o join subjects su on su.subject_id=o.subject_id
    join curricula cu on cu.curriculum_id=o.curriculum_id
    join academic_levels l on l.level_id=o.level_id
    where o.status='PUBLISHED' and su.status='ACTIVE' and cu.status='ACTIVE'
    """;

  private static final String STATS = """
    count(distinct question_id) as "questionCount",
    count(distinct case when question_set=1 then question_id end) as "set1Count",
    count(distinct case when question_set=2 then question_id end) as "set2Count",
    count(distinct case when question_set=1 and difficulty='FOUNDATION' then question_id end) as "set1Easy",
    count(distinct case when question_set=1 and difficulty='INTERMEDIATE' then question_id end) as "set1Medium",
    count(distinct case when question_set=1 and difficulty='ADVANCED' then question_id end) as "set1Hard",
    count(distinct case when question_set=2 and difficulty='FOUNDATION' then question_id end) as "set2Easy",
    count(distinct case when question_set=2 and difficulty='INTERMEDIATE' then question_id end) as "set2Medium",
    count(distinct case when question_set=2 and difficulty='ADVANCED' then question_id end) as "set2Hard",
    count(distinct case when question_set=1 and question_type<>'WORKED_EXAMPLE' then family_code end) as "set1Families"
    """;

  private final JdbcTemplate db;
  private final CurrentStudent current;
  private final ObjectMapper json;
  private final QuestionDraftValidator validator;

  public QuestionInventoryController(
    JdbcTemplate db,
    CurrentStudent current,
    ObjectMapper json,
    QuestionDraftValidator validator
  ) {
    this.db = db;
    this.current = current;
    this.json = json;
    this.validator = validator;
  }

  private UUID requireAdmin() {
    UUID user = current.id();
    if (!Boolean.TRUE.equals(db.queryForObject(
      "select content_admin from app_users where user_id=?", Boolean.class, user
    ))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Content administration permission is required");
    return user;
  }

  private record Scope(String sql, List<Object> args) {
    Object[] params() {
      return args.toArray();
    }

    Scope filter(String condition, Object... values) {
      var next = new ArrayList<>(args);
      next.addAll(List.of(values));
      return new Scope(sql + condition, next);
    }
  }

  private Scope scope(UUID offering, UUID chapter, UUID section, UUID objective) {
    var scope = new Scope("""
      select p.question_id,o.offering_id,c.chapter_id,s.section_id,p.objective_id,
        p.family_code,p.question_set,q.question_type,coalesce(p.difficulty_override,q.difficulty_level) as difficulty
      """ + PLACEMENTS + ACTIVE, List.of());
    if (offering != null) scope = scope.filter(" and o.offering_id=?", offering);
    if (chapter != null) scope = scope.filter(" and c.chapter_id=?", chapter);
    if (section != null) scope = scope.filter(" and s.section_id=?", section);
    if (objective != null) scope = scope.filter(" and p.objective_id=?", objective);
    return scope;
  }

  @GetMapping
  public Map<String, Object> inventory(
    @RequestParam(required = false) UUID offeringId,
    @RequestParam(required = false) UUID chapterId,
    @RequestParam(required = false) UUID sectionId,
    @RequestParam(required = false) UUID objectiveId
  ) {
    requireAdmin();
    var scope = scope(offeringId, chapterId, sectionId, objectiveId);
    var counts = db.queryForMap(
      "with bank as (" + scope.sql() + ") select " + STATS + """
      ,count(distinct case when objective_id is null then question_id end) as "unmappedQuestions"
      from bank
      """, scope.params()
    );
    counts.put("openIssues", db.queryForObject(
      "with bank as (" + scope.sql() + ") select count(*) from question_issue_reports i " +
      "join session_questions sq on sq.session_question_id=i.session_question_id " +
      "where not i.reviewed and exists(select 1 from bank b where b.question_id=sq.question_id)",
      Long.class, scope.params()
    ));

    counts.put("pendingReview", pendingReview(offeringId, chapterId, sectionId, objectiveId));
    counts.putAll(db.queryForMap("""
      with bank as (
      """ + scope.sql() + """
      ), families as (
        select objective_id,count(distinct family_code) as count
        from bank where question_set=1 and question_type<>'WORKED_EXAMPLE' group by objective_id
      )
      select count(*) as "requiredObjectives",
        count(case when coalesce(f.count,0)=0 then 1 end) as "missingObjectives",
        count(case when f.count=1 then 1 end) as "singleFamilyObjectives"
      from learning_objectives a
      join sections s on s.section_id=a.section_id join chapters c on c.chapter_id=s.chapter_id
      join subject_offerings o on o.offering_id=c.offering_id
      left join families f on f.objective_id=a.objective_id
      where a.required and s.status='ACTIVE' and c.status='ACTIVE' and o.status='PUBLISHED'
        and (cast(? as uuid) is null or o.offering_id=cast(? as uuid))
        and (cast(? as uuid) is null or c.chapter_id=cast(? as uuid))
        and (cast(? as uuid) is null or s.section_id=cast(? as uuid))
        and (cast(? as uuid) is null or a.objective_id=cast(? as uuid))
      """, java.util.stream.Stream.concat(
        scope.args().stream(), java.util.stream.Stream.of(
          offeringId, offeringId, chapterId, chapterId, sectionId, sectionId, objectiveId, objectiveId
        )
      ).toArray()
    ));

    if (offeringId == null) return Map.of(
      "counts", counts,
      "children", db.queryForList(
        "with bank as (" + scope.sql() + "), stats as (select offering_id," + STATS +
        " from bank group by offering_id), courses as (" + COURSES + ") " +
        "select co.*,st.* from courses co left join stats st on st.offering_id=co.\"offeringId\" " +
        "order by co.\"curriculumName\",co.\"subjectName\",co.\"trackCode\",co.\"syllabusVersion\"",
        scope.params()
      )
    );

    var args = new ArrayList<>(scope.args());
    String group;
    String children;
    if (chapterId == null) {
      group = "chapter_id";
      children = """
        select c.chapter_id as "chapterId",c.title,c.code,st.* from chapters c
        left join stats st on st.chapter_id=c.chapter_id
        where c.offering_id=? and c.status='ACTIVE' order by c.sort_order,c.title,c.chapter_id
        """;
      args.add(offeringId);
    } else if (sectionId == null) {
      group = "section_id";
      children = """
        select s.section_id as "sectionId",s.title,s.code,st.* from sections s join chapters c on c.chapter_id=s.chapter_id
        left join stats st on st.section_id=s.section_id
        where c.offering_id=? and s.chapter_id=? and c.status='ACTIVE' and s.status='ACTIVE'
        order by s.sort_order,s.title,s.section_id
        """;
      args.add(offeringId);
      args.add(chapterId);
    } else if (objectiveId == null) {
      group = "objective_id";
      children = """
        select a.objective_id as "objectiveId",a.title,a.code,a.required,st.* from learning_objectives a
        join sections s on s.section_id=a.section_id join chapters c on c.chapter_id=s.chapter_id
        left join stats st on st.objective_id=a.objective_id
        where c.offering_id=? and c.chapter_id=? and s.section_id=? and c.status='ACTIVE' and s.status='ACTIVE'
        order by a.required desc,a.code
        """;
      args.add(offeringId);
      args.add(chapterId);
      args.add(sectionId);
    } else return Map.of("counts", counts, "children", List.of());
    return Map.of(
      "counts", counts,
      "children", db.queryForList(
        "with bank as (" + scope.sql() + "), stats as (select " + group + "," + STATS +
        " from bank group by " + group + ") " + children,
        args.toArray()
      )
    );
  }

  private long pendingReview(UUID offering, UUID chapter, UUID section, UUID objective) {
    var banks = db.queryForList("""
      select b.manifest from question_bank_imports b
      join subject_offerings o on o.offering_id=b.offering_id
      where b.status='REVIEW' and o.status='PUBLISHED'
        and (cast(? as uuid) is null or o.offering_id=cast(? as uuid))
      """, String.class, offering, offering);
    if (chapter == null) return banks.size();
    var codes = db.queryForList("""
      select c.code as chapter_code,s.code as section_code,a.code as objective_code
      from chapters c
      left join sections s on s.chapter_id=c.chapter_id and s.section_id=?
      left join learning_objectives a on a.section_id=s.section_id and a.objective_id=?
      where c.chapter_id=? and c.offering_id=?
      """, section, objective, chapter, offering);
    if (codes.isEmpty()) return 0;
    var row = codes.get(0);
    if ((section != null && row.get("section_code") == null) ||
        (objective != null && row.get("objective_code") == null)) return 0;
    return banks.stream().map(this::parse).filter(bank ->
      matchesPending(bank, row.get("chapter_code").toString(),
        (String) row.get("section_code"), (String) row.get("objective_code"))
    ).count();
  }

  private boolean matchesPending(JsonNode bank, String chapterCode, String sectionCode, String objectiveCode) {
    for (JsonNode chapter : bank.path("chapters")) if (chapterCode.equals(chapter.path("code").asText())) {
      if (sectionCode == null) return true;
      for (JsonNode section : chapter.path("topics")) if (sectionCode.equals(section.path("code").asText())) {
        if (objectiveCode == null) return true;
        for (JsonNode objective : section.path("objectives")) {
          if (objectiveCode.equals(objective.path("code").asText())) return true;
        }
        for (JsonNode question : section.path("questions")) {
          if (objectiveCode.equals(question.path("objectiveCode").asText())) return true;
        }
      }
    }
    return false;
  }

  @GetMapping("/questions")
  public Map<String, Object> questions(
    @RequestParam UUID offeringId,
    @RequestParam(required = false) UUID chapterId,
    @RequestParam(required = false) UUID sectionId,
    @RequestParam(required = false) UUID objectiveId,
    @RequestParam(required = false) Integer set,
    @RequestParam(required = false) String difficulty,
    @RequestParam(defaultValue = "") String query,
    @RequestParam(defaultValue = "false") boolean unmapped,
    @RequestParam(defaultValue = "0") int page
  ) {
    requireAdmin();
    if (page < 0 || page > 100000 || query.length() > 200 ||
        (set != null && set != 1 && set != 2) ||
        (difficulty != null && !List.of("FOUNDATION", "INTERMEDIATE", "ADVANCED").contains(difficulty))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Check the question filters");
    }
    var scope = scope(offeringId, chapterId, sectionId, objectiveId);
    if (set != null) scope = scope.filter(" and p.question_set=?", set);
    if (difficulty != null) scope = scope.filter(" and coalesce(p.difficulty_override,q.difficulty_level)=?", difficulty);
    if (unmapped) scope = scope.filter(" and p.objective_id is null");
    if (!query.isBlank()) {
      String term = "%" + query.strip().toLowerCase(Locale.ROOT)
        .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
      scope = scope.filter("""
         and (lower(q.question_text) like ? escape '!' or exists(
           select 1 from external_question_keys k where k.question_id=q.question_id and lower(k.external_id) like ? escape '!'
         ) or exists(
           select 1 from question_sources source where source.question_id=q.question_id
             and lower(source.source_note) like ? escape '!'
         ))
        """, term, term, "%manual bank: " + term.substring(1));
    }
    String bank = "with bank as (" + scope.sql() + ") ";
    var total = db.queryForObject(bank + "select count(distinct question_id) from bank", Long.class, scope.params());
    var args = new ArrayList<>(scope.args());
    args.add(page * 20);
    var items = db.queryForList(bank + """
      select q.question_id as "questionId",q.question_text as prompt,
        min(b.question_set) as "firstSet",max(b.question_set) as "lastSet",
        min(b.difficulty) as "firstDifficulty",max(b.difficulty) as "lastDifficulty"
      from bank b join questions q on q.question_id=b.question_id
      group by q.question_id,q.question_text
      order by lower(q.question_text),q.question_id limit 20 offset ?
      """, args.toArray());
    return Map.of("items", items, "total", total, "page", page, "pageSize", 20);
  }

  @GetMapping("/questions/{id}")
  public Map<String, Object> question(@PathVariable UUID id) {
    requireAdmin();
    var state = revision(id, false);
    var placements = db.queryForList(
      """
      select p.placement_id as "placementId",o.offering_id as "offeringId",o.track_code as "trackCode",
        su.name as "subjectName",cu.name as "curriculumName",l.name as "levelName",
        c.title as "chapterTitle",s.title as "sectionTitle",s.section_id as "sectionId",
        p.question_set as "questionSet",coalesce(p.difficulty_override,q.difficulty_level) as difficulty,
        p.objective_id as "objectiveId",p.family_code as "familyCode",a.title as "objectiveTitle"
      """ + PLACEMENTS + """
      join subjects su on su.subject_id=o.subject_id
      join curricula cu on cu.curriculum_id=o.curriculum_id
      join academic_levels l on l.level_id=o.level_id
      left join learning_objectives a on a.objective_id=p.objective_id
      """ + ACTIVE + " and p.question_id=? order by cu.name,su.name,l.name,o.track_code,c.sort_order,s.sort_order,p.question_set,p.placement_id",
      id
    );
    var objectives = db.queryForList("""
      select distinct a.objective_id as "objectiveId",a.section_id as "sectionId",a.code,a.title
      from learning_objectives a join question_placements p on p.section_id=a.section_id
      where p.question_id=? and p.active order by a.title,a.code
      """, id);
    var sources = db.queryForList("""
      select distinct d.title,d.asset_id as "assetId",d.document_type as "documentType",
        s.page_number as "pageNumber",s.source_note as note
      from question_sources s left join content_documents d on d.document_id=s.document_id
      where s.question_id=? order by d.title,s.page_number,s.source_note
      """, id);
    var revisions = db.queryForList("""
      select r.revision_number as "revisionNumber",r.created_at as "createdAt",
        (select max(v.review_note) from question_reviews v where v.revision_id=r.revision_id) as note
      from question_revisions r where r.question_id=? order by r.revision_number desc
      """, id);
    return Map.of(
      "question", parse(state.get("content_snapshot")),
      "revisionNumber", state.get("current_revision"),
      "placements", placements,
      "objectives", objectives,
      "sources", sources,
      "revisions", revisions
    );
  }

  @GetMapping("/questions/{id}/revisions/{number}")
  public JsonNode previousRevision(@PathVariable UUID id, @PathVariable int number) {
    requireAdmin();
    revision(id, false);
    var rows = db.queryForList(
      "select content_snapshot from question_revisions where question_id=? and revision_number=?",
      String.class, id, number
    );
    if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Revision not found");
    return parse(rows.get(0));
  }

  private Map<String, Object> revision(UUID id, boolean lock) {
    if (lock) db.queryForList("select question_id from questions where question_id=? for update", id);
    var rows = db.queryForList("""
      select q.current_revision,r.content_snapshot
      from questions q join question_revisions r on r.question_id=q.question_id and r.revision_number=q.current_revision
      where q.question_id=? and q.approval_status='PUBLISHED' and q.visibility='GLOBAL'
      """, id);
    if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Published question not found");
    return rows.get(0);
  }

  private ObjectNode parse(Object stored) {
    try {
      return (ObjectNode) json.readTree(stored.toString());
    } catch (IOException e) {
      throw new IllegalStateException("Invalid question revision", e);
    }
  }

  public record Correction(
    int expectedRevision,
    boolean reviewConfirmed,
    String note,
    JsonNode content
  ) {}

  @PutMapping("/questions/{id}")
  @Transactional
  public Map<String, Object> correct(@PathVariable UUID id, @RequestBody Correction request) {
    UUID reviewer = requireAdmin();
    if (!request.reviewConfirmed() || request.note() == null || request.note().isBlank() ||
        request.note().length() > 300 || request.content() == null || !request.content().isObject()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirm your review and enter a correction note of up to 300 characters");
    }
    var state = revision(id, true);
    int currentRevision = ((Number) state.get("current_revision")).intValue();
    if (request.expectedRevision() != currentRevision) throw new ResponseStatusException(
      HttpStatus.CONFLICT, "Another editor updated this question. Reopen it to see the latest version."
    );
    var snapshot = parse(state.get("content_snapshot"));
    var before = snapshot.deepCopy();
    for (String field : List.of("prompt", "options", "correctAnswer", "correctAnswers", "acceptedAnswers", "explanation", "hint")) {
      if (request.content().has(field)) snapshot.set(field, request.content().get(field));
    }
    try {
      validator.validate(snapshot);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
    if (snapshot.equals(before)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Change at least one question field before publishing");
    UUID revisionId = UUID.randomUUID();
    int next = currentRevision + 1;
    db.update(
      "insert into question_revisions(revision_id,question_id,revision_number,content_snapshot,reviewed_by) values(?,?,?,?,?)",
      revisionId, id, next, snapshot.toString(), reviewer
    );
    db.update("""
      update questions set question_text=?,correct_answer=?,correct_answers=?,explanation=?,hint=?,
        current_revision=?,updated_at=current_timestamp where question_id=?
      """,
      snapshot.path("prompt").asText(), snapshot.path("correctAnswer").asText(null),
      snapshot.has("correctAnswers") ? snapshot.get("correctAnswers").toString() : null,
      snapshot.path("explanation").asText(), snapshot.path("hint").asText(null), next, id
    );
    db.update("""
      insert into question_reviews(question_id,revision_id,reviewer_id,decision,review_note) values(?,?,?,'APPROVED',?)
      """, id, revisionId, reviewer, request.note().strip());
    return Map.of("revisionNumber", next);
  }

  public record ObjectiveMapping(
    UUID expectedObjectiveId,
    String expectedFamilyCode,
    UUID objectiveId,
    String familyCode
  ) {}

  @PutMapping("/placements/{id}/objective")
  @Transactional
  public void mapObjective(@PathVariable UUID id, @RequestBody ObjectiveMapping request) {
    requireAdmin();
    String family = request.familyCode() == null ? null : request.familyCode().strip();
    if (request.objectiveId() == null) family = null;
    else if (family == null || !family.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,119}")) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST, "Enter a family code using letters, digits, dots, underscores or hyphens"
    );
    db.queryForList("select placement_id from question_placements where placement_id=? for update", id);
    var rows = db.queryForList(
      "select p.section_id,p.objective_id,p.family_code " + PLACEMENTS + ACTIVE + " and p.placement_id=?", id
    );
    if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Active question placement not found");
    var row = rows.get(0);
    if (!Objects.equals(row.get("objective_id"), request.expectedObjectiveId()) ||
        !Objects.equals(row.get("family_code"), request.expectedFamilyCode())) throw new ResponseStatusException(
      HttpStatus.CONFLICT, "This mapping changed. Reopen the question before editing it again."
    );
    if (request.objectiveId() != null && !Boolean.TRUE.equals(db.queryForObject(
      "select exists(select 1 from learning_objectives where objective_id=? and section_id=?)",
      Boolean.class, request.objectiveId(), row.get("section_id")
    ))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an objective from this subtopic");
    db.update("update question_placements set objective_id=?,family_code=? where placement_id=?", request.objectiveId(), family, id);
  }
}
