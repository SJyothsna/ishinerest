package com.istudy.learning;

import static com.istudy.learning.LearningModels.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SessionService {

  private final JdbcTemplate db;
  private final ObjectMapper json;
  private final QuestionSelector selector;
  private final Grader grader;
  private final SetProgressionService progression;
  private final LearningProfileService profiles;

  public SessionService(
    JdbcTemplate db,
    ObjectMapper json,
    QuestionSelector selector,
    Grader grader,
    SetProgressionService progression,
    LearningProfileService profiles
  ) {
    this.db = db;
    this.json = json;
    this.selector = selector;
    this.grader = grader;
    this.progression = progression;
    this.profiles = profiles;
  }

  JsonNode parse(String value) {
    try {
      return json.readTree(value);
    } catch (Exception e) {
      throw new IllegalStateException("Invalid stored question snapshot", e);
    }
  }

  String encode(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JSON payload");
    }
  }

  private void require(boolean condition, String reason) {
    if (!condition) throw new ResponseStatusException(
      HttpStatus.UNPROCESSABLE_ENTITY,
      reason
    );
  }

  @Transactional
  public Map<String, Object> create(
    UUID student,
    UUID guest,
    CreateSession request
  ) {
    require(
      Set.of("PRACTICE", "TEST", "INITIAL_ASSESSMENT", "SAMPLE").contains(
        request.kind()
      ),
      "Unsupported session kind"
    );
    require(
      Set.of("QUICK", "FLAGGED", "MISTAKES", "PERSONALIZED").contains(request.mode()),
      "Unsupported practice mode"
    );
    require(
      Set.of(
        "SUBJECT_ALL",
        "SUBJECT_STARTED",
        "CHAPTER_ALL",
        "SELECTED_SECTIONS"
      ).contains(request.scope()),
      "Invalid practice scope"
    );
    require(
      Set.of("ALL", "FOUNDATION", "INTERMEDIATE", "ADVANCED").contains(
        request.difficulty()
      ),
      "Invalid difficulty"
    );
    require(
      guest == null || request.kind().equals("SAMPLE"),
      "Guests may create sample sessions only"
    );
    require(
      student == null || !request.kind().equals("SAMPLE"),
      "Samples require guest ownership"
    );
    require(
      !request.scope().equals("SUBJECT_STARTED") || student != null,
      "Started chapters require practice history"
    );
    var sections = request.sectionIds() == null
      ? List.<UUID>of()
      : request.sectionIds();
    require(sections.size() <= 100, "Choose no more than 100 topics");
    require(
      request.scope().equals("SELECTED_SECTIONS")
        ? !sections.isEmpty()
        : sections.isEmpty(),
      "Select topics only for specific-topic scope"
    );
    require(
      !request.scope().equals("CHAPTER_ALL") || request.chapterId() != null,
      "Choose a chapter"
    );
    require(
      request.scope().equals("CHAPTER_ALL") ||
      request.scope().equals("SELECTED_SECTIONS") ||
      request.chapterId() == null,
      "Chapter does not match subject scope"
    );
    if (student != null) require(
      Boolean.TRUE.equals(
        db.queryForObject(
          "select exists(select 1 from student_subjects where student_id=? and offering_id=? and status='ENROLLED')",
          Boolean.class,
          student,
          request.offeringId()
        )
      ),
      "Enrol in this subject first"
    );
    if (request.chapterId() != null) require(
      Boolean.TRUE.equals(
        db.queryForObject(
          "select exists(select 1 from chapters where chapter_id=? and offering_id=? and status='ACTIVE')",
          Boolean.class,
          request.chapterId(),
          request.offeringId()
        )
      ),
      "Chapter does not belong to this subject"
    );
    for (UUID section : sections) require(
      Boolean.TRUE.equals(
        db.queryForObject(
          "select exists(select 1 from sections s join chapters c on c.chapter_id=s.chapter_id where s.section_id=? and c.offering_id=? and (?::uuid is null or c.chapter_id=?::uuid) and s.status='ACTIVE' and c.status='ACTIVE')",
          Boolean.class,
          section,
          request.offeringId(),
          request.chapterId(),
          request.chapterId()
        )
      ),
      "Topic does not belong to the selected scope"
    );
    boolean personalized = request.mode().equals("PERSONALIZED");
    require(
      !personalized || (student != null && Set.of("PRACTICE", "TEST").contains(request.kind())),
      "Personalized selection requires a student practice session or test"
    );
    if (request.objectiveId() != null) require(
      personalized && request.kind().equals("PRACTICE") && sections.size() == 1 &&
      Boolean.TRUE.equals(db.queryForObject(
        "select exists(select 1 from learning_objectives where objective_id=? and section_id=?)",
        Boolean.class, request.objectiveId(), sections.get(0)
      )),
      "Choose a practice objective in the selected topic"
    );
    if (student != null) lockStudent(student);
    String sql =
      """
      select q.question_id,r.revision_id,p.placement_id,p.question_set,s.section_id,c.chapter_id,r.content_snapshot,
      p.objective_id,p.family_code,coalesce(p.difficulty_override,q.difficulty_level) as effective_difficulty,
      exists(select 1 from session_questions seenq join practice_sessions seenps on seenps.session_id=seenq.session_id
        where (seenps.student_id=? or seenps.claimed_by=?) and seenps.offering_id=c.offering_id and seenq.question_id=q.question_id) as seen,
      exists(select 1 from session_questions seenq join practice_sessions seenps on seenps.session_id=seenq.session_id
        where (seenps.student_id=? or seenps.claimed_by=?) and seenps.offering_id=c.offering_id
          and seenq.objective_id=p.objective_id and seenq.family_code=p.family_code) as family_seen,
      coalesce(st.total_attempts,0) as attempts,coalesce(st.correct_attempts,0) as correct,
      exists(select 1 from student_question_flags f where f.student_id=? and f.offering_id=c.offering_id and f.question_id=q.question_id) as flagged
      from question_placements p join questions q on q.question_id=p.question_id
      join question_revisions r on r.question_id=q.question_id and r.revision_number=q.current_revision
      join sections s on s.section_id=p.section_id join chapters c on c.chapter_id=s.chapter_id join subject_offerings o on o.offering_id=c.offering_id
      left join student_question_stats st on st.question_id=q.question_id and st.offering_id=c.offering_id and st.student_id=?
      where c.offering_id=? and o.status='PUBLISHED' and c.status='ACTIVE' and s.status='ACTIVE' and p.active
      and q.approval_status='PUBLISHED' and q.visibility='GLOBAL'
      and (?::uuid is null or p.objective_id=?::uuid)
      and (?='ALL' or coalesce(p.difficulty_override,q.difficulty_level)=?)
      and (?::uuid is null or c.chapter_id=?::uuid)
      and (? <> 'SUBJECT_STARTED' or exists(select 1 from question_attempts a join session_questions sq on sq.session_question_id=a.session_question_id
        join question_placements pp on pp.placement_id=sq.placement_id join sections ss on ss.section_id=pp.section_id
        join practice_sessions started on started.session_id=sq.session_id
        where a.student_id=? and started.offering_id=c.offering_id and ss.chapter_id=c.chapter_id))
      and (q.question_type<>'WORKED_EXAMPLE' or (?='PRACTICE' and ?<>'PERSONALIZED'))
      and (?<>'FLAGGED' or exists(select 1 from student_question_flags f where f.student_id=? and f.offering_id=c.offering_id and f.question_id=q.question_id))
      and (?<>'MISTAKES' or coalesce(st.total_attempts,0)>coalesce(st.correct_attempts,0))
      and (? in ('SAMPLE','INITIAL_ASSESSMENT') or p.usage_type in ('BOTH','ALL') or p.usage_type=case when ?='TEST' then 'TEST' else 'PRACTICE' end)
      order by coalesce(st.total_attempts,0),s.sort_order,q.question_id limit 5000
      """;
    var arguments = new ArrayList<Object>(
      Arrays.asList(
        student,
        student,
        student,
        student,
        student,
        student,
        request.offeringId(),
        request.objectiveId(),
        request.objectiveId(),
        request.difficulty(),
        request.difficulty(),
        request.chapterId(),
        request.chapterId(),
        request.scope(),
        student,
        request.kind(),
        request.mode(),
        request.mode(),
        student,
        request.mode(),
        request.kind(),
        request.kind()
      )
    );
    if (!sections.isEmpty()) {
      sql = sql.replace(
        "order by coalesce(st.total_attempts,0)",
        "and s.section_id in (" +
        String.join(",", Collections.nCopies(sections.size(), "?")) +
        ") order by coalesce(st.total_attempts,0)"
      );
      arguments.addAll(sections);
    }
    if (request.kind().equals("SAMPLE")) sql = sql.replace(" limit 5000", "");
    var pool = db.query(
      sql,
      (rs, n) ->
        new Candidate(
          rs.getObject("question_id", UUID.class),
          rs.getObject("revision_id", UUID.class),
          rs.getObject("placement_id", UUID.class),
          rs.getObject("section_id", UUID.class),
          rs.getObject("chapter_id", UUID.class),
          parse(rs.getString("content_snapshot")),
          rs.getInt("attempts"),
          rs.getInt("correct"),
          rs.getBoolean("flagged"),
          rs.getInt("question_set"),
          rs.getBoolean("seen"),
          rs.getBoolean("family_seen"),
          rs.getObject("objective_id", UUID.class),
          rs.getString("family_code"),
          rs.getString("effective_difficulty")
        ),
      arguments.toArray()
    );
    pool = pool
      .stream()
      .filter(
        c ->
          request.kind().equals("PRACTICE") ||
          !c.snapshot().path("type").asText().equals("worked")
      )
      .filter(c -> sections.isEmpty() || sections.contains(c.sectionId()))
      .filter(c -> !request.mode().equals("FLAGGED") || c.flagged())
      .filter(
        c -> !request.mode().equals("MISTAKES") || c.attempts() > c.correct()
      )
      .toList();
    var selected = new ArrayList<Candidate>();
    var selectedFamilies = new HashSet<String>();
    var evidence = new HashMap<UUID, LearningProfileService.Evidence>();
    if (personalized) profiles.profile(student, request.offeringId(), request.chapterId())
      .objectives().forEach(item -> evidence.put(item.coverageKey(), item));
    boolean balancedTest = personalized && request.kind().equals("TEST");
    if ((request.mode().equals("QUICK") || personalized) && !request.kind().equals("SAMPLE")) {
      for (
        int stage = 0;
        stage < 4 && selected.size() < request.questionCount();
        stage++
      ) {
        final int bucket = stage;
        var ids = new HashSet<UUID>();
        selected.forEach(c -> ids.add(c.questionId()));
        var candidates = pool
          .stream()
          .filter(c -> !ids.contains(c.questionId()))
          .filter(c ->
            bucket == 0
              ? c.questionSet() == 1 && !c.seen()
              : bucket == 1
                ? c.questionSet() == 2 && !c.seen()
                : bucket == 2 ? c.questionSet() == 1 : c.questionSet() == 2
          )
          .toList();
        selected.addAll(
          selector.select(candidates, request.questionCount() - selected.size(),
            selectedFamilies, evidence, balancedTest)
        );
      }
    } else selected.addAll(
      request.kind().equals("SAMPLE")
        ? selector.sample(pool, request.questionCount())
        : selector.select(pool, request.questionCount(), selectedFamilies, evidence, balancedTest)
    );
    require(
      !selected.isEmpty(),
      "No eligible questions match this scope, mode and difficulty. Change the selection to continue."
    );
    // Shortage is explicit; never widen a chosen scope/difficulty.
    UUID id = UUID.randomUUID(), token = UUID.randomUUID();
    db.update(
      "insert into practice_sessions(session_id,student_id,guest_id,offering_id,chapter_id,session_kind,practice_mode,scope_type,difficulty_filter,requested_count,selected_count,timer_seconds,status,started_at,expires_at,submission_token) values(?,?,?,?,?,?,?,?,?,?,?,?, 'IN_PROGRESS',now(),?,?)",
      id,
      student,
      guest,
      request.offeringId(),
      request.chapterId(),
      request.kind(),
      request.mode(),
      request.scope(),
      request.difficulty(),
      request.questionCount(),
      selected.size(),
      request.timerSeconds(),
      request.timerSeconds() == null
        ? null
        : java.sql.Timestamp.from(
          java.time.Instant.now().plusSeconds(request.timerSeconds())
        ),
      token
    );
    sections.forEach(section ->
      db.update(
        "insert into practice_session_sections values(?,?)",
        id,
        section
      )
    );
    int position = 1;
    for (var c : selected) db.update(
      "insert into session_questions(session_id,question_id,revision_id,placement_id,question_position,objective_id,family_code,effective_difficulty,previously_seen,family_previously_seen) values(?,?,?,?,?,?,?,?,?,?)",
      id,
      c.questionId(),
      c.revisionId(),
      c.placementId(),
      position++,
      c.objectiveId(),
      c.familyCode(),
      c.difficulty(),
      student == null ? null : c.seen(),
      student == null ? null : c.familySeen()
    );
    db.update(
      "update practice_sessions set repeated_count=? where session_id=?",
      selected.stream().filter(Candidate::seen).count(),
      id
    );
    if (
      student != null &&
      request.kind().equals("PRACTICE") &&
      (request.mode().equals("QUICK") || personalized)
    ) progression.prepare(
      student,
      request.offeringId(),
      selected.stream().map(Candidate::chapterId).toList()
    );
    return read(id, student, guest);
  }

  private Map<String, Object> owned(
    UUID id,
    UUID student,
    UUID guest,
    boolean lock
  ) {
    var rows = db.queryForList(
      "select * from practice_sessions where session_id=? and ((student_id=? and ?::uuid is not null) or (guest_id=? and ?::uuid is not null))" +
      (lock ? " for update" : ""),
      id,
      student,
      student,
      guest,
      guest
    );
    if (rows.isEmpty()) throw new ResponseStatusException(
      HttpStatus.NOT_FOUND,
      "Session not found"
    );
    return rows.get(0);
  }

  public Map<String, Object> read(UUID id, UUID student, UUID guest) {
    var s = owned(id, student, guest, false);
    var result = new LinkedHashMap<String, Object>();
    result.put("sessionId", id);
    result.put("kind", s.get("session_kind"));
    result.put("practiceMode", s.get("practice_mode"));
    result.put("status", s.get("status"));
    result.put("submissionToken", s.get("submission_token"));
    result.put("expiresAt", s.get("expires_at"));
    result.put("serverTime", java.time.Instant.now());
    result.put("timerSeconds", s.get("timer_seconds"));
    result.putAll(
      db.queryForMap(
        "select sub.name as \"subjectName\",l.name as \"levelName\",o.track_code as \"trackCode\",c.title as \"chapterName\" from subject_offerings o join subjects sub on sub.subject_id=o.subject_id join academic_levels l on l.level_id=o.level_id left join chapters c on c.chapter_id=? where o.offering_id=?",
        s.get("chapter_id"),
        s.get("offering_id")
      )
    );
    result.put("requestedCount", s.get("requested_count"));
    result.put("repeatedCount", s.get("repeated_count"));
    result.put("selectedCount", s.get("selected_count"));
    result.put("offeringId", s.get("offering_id"));
    result.put(
      "isStarter",
      db.queryForObject(
        "select exists(select 1 from session_questions sq join question_placements p on p.placement_id=sq.placement_id where sq.session_id=? and p.is_starter)",
        Boolean.class,
        id
      )
    );
    var flags = student == null
      ? Set.<UUID>of()
      : new HashSet<>(
        db.queryForList(
          "select question_id from student_question_flags where student_id=? and offering_id=?",
          UUID.class,
          student,
          s.get("offering_id")
        )
      );
    result.put("chapterId", s.get("chapter_id"));
    result.put(
      "shortage",
      !s.get("requested_count").equals(s.get("selected_count"))
    );
    JsonNode privateResults = s.get("result_payload") == null
      ? json.createObjectNode()
      : parse(s.get("result_payload").toString());
    if (s.get("result_payload") != null) {
      var basic = privateResults.deepCopy();
      ((ObjectNode) basic).remove("gradedAnswers");
      result.put("result", basic);
    }
    boolean review =
      s.get("status").equals("SUBMITTED") &&
      (guest == null ||
        (student != null && student.equals(s.get("claimed_by"))));
    var questions = db.query(
      "select sq.session_question_id,sq.question_id,sq.review_flag,sq.effective_difficulty,ck.first_response_payload,ck.is_correct as first_correct,(select sec.title from question_placements pl join sections sec on sec.section_id=pl.section_id where pl.placement_id=sq.placement_id) as topic_name,(select ch.title from question_placements pl join sections sec on sec.section_id=pl.section_id join chapters ch on ch.chapter_id=sec.chapter_id where pl.placement_id=sq.placement_id) as chapter_name,(select p.question_set from question_placements p where p.placement_id=sq.placement_id) as question_set,r.content_snapshot,d.response_payload as draft,a.response_payload as answer,a.is_correct from session_questions sq join question_revisions r on r.revision_id=sq.revision_id left join session_answer_drafts d on d.session_question_id=sq.session_question_id left join session_answer_checks ck on ck.session_question_id=sq.session_question_id left join question_attempts a on a.session_question_id=sq.session_question_id where sq.session_id=? order by sq.question_position",
      (rs, n) -> {
        var snapshot = parse(rs.getString("content_snapshot"));
        var q = json.createObjectNode();
        q.put(
          "sessionQuestionId",
          rs.getObject("session_question_id").toString()
        );
        q.put("questionId", rs.getObject("question_id").toString());
        q.put("questionSet", rs.getInt("question_set"));
        q.put(
          "flagged",
          guest == null
            ? flags.contains(rs.getObject("question_id", UUID.class))
            : rs.getBoolean("review_flag")
        );
        q.put("topicName", rs.getString("topic_name"));
        q.put("chapterName", rs.getString("chapter_name"));
        if (
          s.get("session_kind").equals("PRACTICE") &&
          rs.getString("first_response_payload") != null
        ) {
          q.set(
            "feedback",
            json.valueToTree(
              checkedFeedback(
                snapshot,
                rs.getObject("session_question_id", UUID.class),
                parse(rs.getString("first_response_payload"))
              )
            )
          );
          q.put("firstCheckCorrect", rs.getBoolean("first_correct"));
        }
        for (String field : List.of(
          "prompt",
          "type",
          "options",
          "imageAssetId",
          "difficulty",
          "examLevel"
        )) if (snapshot.has(field)) q.set(field, snapshot.get(field));
        if (rs.getString("effective_difficulty") != null) {
          q.put("difficulty", rs.getString("effective_difficulty"));
        }
        if (rs.getString("draft") != null) q.set(
          "draft",
          parse(rs.getString("draft"))
        );
        if (review) {
          q.set(
            "videos",
            json.valueToTree(
              explanationVideos(rs.getObject("session_question_id", UUID.class))
            )
          );
          for (String field : List.of(
            "correctAnswer",
            "correctAnswers",
            "explanation"
          )) if (snapshot.has(field)) q.set(field, snapshot.get(field));
          if (rs.getString("answer") != null) q.set(
            "answer",
            parse(rs.getString("answer"))
          );
          q.put("correct", rs.getBoolean("is_correct"));
          if (guest != null) for (var graded : privateResults.path(
            "gradedAnswers"
          )) if (
            graded
              .path("sessionQuestionId")
              .asText()
              .equals(rs.getObject("session_question_id").toString())
          ) {
            q.set("answer", graded.path("answer"));
            q.put("correct", graded.path("correct").asBoolean());
          }
        }
        return q;
      },
      id
    );
    result.put("questions", questions);
    return result;
  }

  @Transactional
  public void drafts(UUID id, UUID student, UUID guest, List<Answer> answers) {
    var s = owned(id, student, guest, true);
    editable(s);
    validateAnswers(id, answers);
    for (var answer : answers) {
      if (
        db.update(
          "update session_answer_drafts set response_payload=?,saved_at=current_timestamp where session_question_id=?",
          answer.response().toString(),
          answer.sessionQuestionId()
        ) ==
        0
      ) db.update(
        "insert into session_answer_drafts(session_question_id,response_payload) values(?,?)",
        answer.sessionQuestionId(),
        answer.response().toString()
      );
    }
  }

  private void validateAnswers(UUID id, List<Answer> answers) {
    var valid = new HashSet<>(
      db.queryForList(
        "select session_question_id from session_questions where session_id=?",
        UUID.class,
        id
      )
    );
    var seen = new HashSet<UUID>();
    for (var a : answers) require(
      a != null &&
      a.sessionQuestionId() != null &&
      a.response() != null &&
      valid.contains(a.sessionQuestionId()) &&
      seen.add(a.sessionQuestionId()),
      "Invalid or repeated session question"
    );
  }

  private java.time.Instant expiry(Object value) {
    return value instanceof java.time.OffsetDateTime t
      ? t.toInstant()
      : ((java.sql.Timestamp) value).toInstant();
  }

  private void editable(Map<String, Object> s) {
    require(
      s.get("status").equals("IN_PROGRESS"),
      "This session is already finished"
    );
    if (s.get("expires_at") != null) require(
      expiry(s.get("expires_at")).isAfter(java.time.Instant.now()),
      "Session time has expired; submit your saved answers"
    );
  }

  private List<Map<String, Object>> explanationVideos(UUID sessionQuestionId) {
    return db.queryForList(
      "select distinct v.video_id as \"videoId\",v.youtube_video_id as \"youtubeVideoId\",v.title from session_questions sq join question_placements p on p.placement_id=sq.placement_id join video_sections vs on vs.section_id=p.section_id join learning_videos v on v.video_id=vs.video_id where sq.session_question_id=? and v.status='ACTIVE' order by v.title limit 5",
      sessionQuestionId
    );
  }

  @Transactional
  public Map<String, Object> check(UUID id, UUID student, Answer answer) {
    var s = owned(id, student, null, true);
    editable(s);
    require(
      s.get("session_kind").equals("PRACTICE"),
      "Answers can only be checked during practice"
    );
    validateAnswers(id, List.of(answer));
    var snapshot = parse(
      db.queryForObject(
        "select r.content_snapshot from session_questions sq join question_revisions r on r.revision_id=sq.revision_id where sq.session_question_id=?",
        String.class,
        answer.sessionQuestionId()
      )
    );
    var previous = db.queryForList(
      "select first_response_payload from session_answer_checks where session_question_id=?",
      String.class,
      answer.sessionQuestionId()
    );
    JsonNode first = previous.isEmpty()
      ? answer.response()
      : parse(previous.get(0));
    if (previous.isEmpty()) {
      require(
        snapshot.path("type").asText().equals("worked") || answered(first),
        "Select an answer before checking"
      );
      drafts(id, student, null, List.of(answer));
      db.update(
        "insert into session_answer_checks(session_question_id,first_response_payload,is_correct) values(?,?,?)",
        answer.sessionQuestionId(),
        first.toString(),
        grader.correct(snapshot, first)
      );
    }
    return checkedFeedback(snapshot, answer.sessionQuestionId(), first);
  }

  private boolean answered(JsonNode answer) {
    return (
      answer != null &&
      !answer.isNull() &&
      !(answer.isTextual() && answer.asText().isBlank()) &&
      !(answer.isArray() && answer.isEmpty())
    );
  }

  private Map<String, Object> checkedFeedback(
    JsonNode snapshot,
    UUID question,
    JsonNode first
  ) {
    var checked = new LinkedHashMap<String, Object>();
    checked.put("correct", grader.correct(snapshot, first));
    checked.put("firstResponse", first);
    checked.put("explanation", snapshot.path("explanation").asText());
    boolean worked = snapshot.path("type").asText().equals("worked");
    checked.put("requiresSelfReview", worked);
    checked.put(
      "modelAnswer",
      worked ? snapshot.path("correctAnswer").asText() : ""
    );
    for (String field : List.of("correctAnswer", "correctAnswers")) if (
      snapshot.has(field)
    ) checked.put(field, snapshot.get(field));
    checked.put("videos", explanationVideos(question));
    return checked;
  }

  private void questionInSession(UUID id, UUID question) {
    require(
      Boolean.TRUE.equals(
        db.queryForObject(
          "select exists(select 1 from session_questions where session_id=? and session_question_id=?)",
          Boolean.class,
          id,
          question
        )
      ),
      "Question does not belong to this session"
    );
  }

  @Transactional
  public void reviewFlag(
    UUID id,
    UUID student,
    UUID guest,
    UUID question,
    boolean flagged
  ) {
    var session = owned(id, student, guest, true);
    require(
      session.get("status").equals("IN_PROGRESS"),
      "This session is already finished"
    );
    questionInSession(id, question);
    db.update(
      "update session_questions set review_flag=? where session_question_id=?",
      flagged,
      question
    );
    if (student != null) {
      UUID canonical = db.queryForObject(
        "select question_id from session_questions where session_question_id=?",
        UUID.class,
        question
      );
      saveFlag(student, (UUID) session.get("offering_id"), canonical, flagged);
    }
  }

  @Transactional
  public void saveFlag(UUID student, UUID offering, UUID question, boolean flagged) {
    // Lock the owner to serialize flag changes across different sessions.
    lockStudent(student);
    if (flagged) db.update(
      "insert into student_question_flags(student_id,offering_id,question_id) select ?,?,? where not exists(select 1 from student_question_flags where student_id=? and offering_id=? and question_id=?)",
      student, offering, question, student, offering, question
    );
    else db.update(
      "delete from student_question_flags where student_id=? and offering_id=? and question_id=?",
      student, offering, question
    );
  }

  @Transactional
  public void reportIssue(
    UUID id,
    UUID student,
    UUID guest,
    UUID question,
    String reason
  ) {
    owned(id, student, guest, true);
    questionInSession(id, question);
    require(
      Set.of(
        "INCORRECT_ANSWER",
        "UNCLEAR_WORDING",
        "MISSING_INFORMATION"
      ).contains(reason),
      "Choose a valid issue reason"
    );
    db.update(
      "insert into question_issue_reports(session_question_id,reason) select ?,? where not exists(select 1 from question_issue_reports where session_question_id=? and reason=?)",
      question,
      reason,
      question,
      reason
    );
  }

  @Transactional
  public Map<String, Object> submit(
    UUID id,
    UUID student,
    UUID guest,
    Submit request
  ) {
    var s = owned(id, student, guest, true);
    require(
      s.get("submission_token").equals(request.submissionToken()),
      "Invalid submission token"
    );
    if (s.get("status").equals("SUBMITTED")) return read(id, student, guest);
    require(s.get("status").equals("IN_PROGRESS"), "Session is not open");
    validateAnswers(id, request.answers());
    var supplied = new HashMap<UUID, JsonNode>();
    boolean expired =
      s.get("expires_at") != null &&
      expiry(s.get("expires_at")).isBefore(java.time.Instant.now());
    if (expired) db.query(
      "select d.session_question_id,d.response_payload from session_answer_drafts d join session_questions sq on sq.session_question_id=d.session_question_id where sq.session_id=?",
      (org.springframework.jdbc.core.RowCallbackHandler) rs ->
        supplied.put(
          rs.getObject("session_question_id", UUID.class),
          parse(rs.getString("response_payload"))
        ),
      id
    );
    else request
      .answers()
      .forEach(a -> supplied.put(a.sessionQuestionId(), a.response()));
    int correct = 0, attempted = 0, selfReviewed =
      0, correctedAfterExplanation = 0;
    var grade = json.createArrayNode();
    for (var row : db.queryForList(
      "select sq.session_question_id,sq.question_id,r.content_snapshot,ck.is_correct as first_correct from session_questions sq join question_revisions r on r.revision_id=sq.revision_id left join session_answer_checks ck on ck.session_question_id=sq.session_question_id where session_id=? order by question_position",
      id
    )) {
      UUID sq = (UUID) row.get("session_question_id");
      var answer = supplied.get(sq);
      if (answer == null) answer = NullNode.instance;
      boolean evaluated =
        !answer.isNull() &&
        !(answer.isTextual() && answer.asText().isBlank()) &&
        !(answer.isArray() && answer.isEmpty());
      boolean selfReview = parse(row.get("content_snapshot").toString())
        .path("type")
        .asText()
        .equals("worked");
      boolean right =
        evaluated &&
        grader.correct(parse(row.get("content_snapshot").toString()), answer);
      if (evaluated) attempted++;
      if (evaluated && selfReview) selfReviewed++;
      if (right && !selfReview) correct++;
      if (
        right && !selfReview && Boolean.FALSE.equals(row.get("first_correct"))
      ) correctedAfterExplanation++;
      var g = json
        .createObjectNode()
        .put("sessionQuestionId", sq.toString())
        .put("correct", right);
      g.set("answer", answer);
      grade.add(g);
      // Clearing a final choice must not erase an already evaluated first attempt.
      if (
        student != null &&
        (evaluated || (!selfReview && row.get("first_correct") != null))
      ) db.update(
        "insert into question_attempts(session_question_id,student_id,response_payload,is_correct,grading_version) values(?,?,?,?,?)",
        sq,
        student,
        answer.toString(),
        right,
        selfReview ? "self-review-v1" : "v1"
      );
    }
    var result = json
      .createObjectNode()
      .put("attempted", attempted)
      .put("correct", correct)
      .put("selfReviewed", selfReviewed)
      .put("total", ((Number) s.get("selected_count")).intValue())
      .put(
        "unanswered",
        ((Number) s.get("selected_count")).intValue() - attempted
      )
      .put("correctedAfterExplanation", correctedAfterExplanation);
    if (attempted > selfReviewed) result.put(
      "accuracy",
      Math.round((correct * 100f) / (attempted - selfReviewed))
    );
    else result.putNull("accuracy");
    // Guest grading is private, not authoritative adaptive history. Claiming only unlocks review.
    result.set("gradedAnswers", grade);
    db.update(
      "update practice_sessions set status='SUBMITTED',submitted_at=now(),result_payload=? where session_id=?",
      result.toString(),
      id
    );
    if (student != null) {
      db.update(
        "update student_subjects set started_at=coalesce(started_at,now()),last_activity_at=now(),last_chapter_id=coalesce(?::uuid,last_chapter_id) where student_id=? and offering_id=?",
        s.get("chapter_id"),
        student,
        s.get("offering_id")
      );
      refreshQuestionStats(student, (UUID) s.get("offering_id"));
    }
    db.update(
      "delete from session_answer_drafts where session_question_id in(select session_question_id from session_questions where session_id=?)",
      id
    );
    return read(id, student, guest);
  }

  private void lockStudent(UUID student) {
    db.queryForObject(
      "select student_id from student_profiles where student_id=? for update",
      UUID.class,
      student
    );
  }

  private void refreshQuestionStats(UUID student, UUID offering) {
    lockStudent(student);
    db.update("delete from student_question_stats where student_id=? and offering_id=?", student, offering);
    db.update(
      """
      insert into student_question_stats(student_id,offering_id,question_id,total_attempts,correct_attempts,last_attempted_at)
      select a.student_id,ps.offering_id,sq.question_id,count(*),sum(case when coalesce(ck.is_correct,a.is_correct) then 1 else 0 end),max(a.submitted_at)
      from question_attempts a join session_questions sq on sq.session_question_id=a.session_question_id
      join practice_sessions ps on ps.session_id=sq.session_id
      left join session_answer_checks ck on ck.session_question_id=sq.session_question_id
      where a.student_id=? and ps.offering_id=? and a.grading_version<>'self-review-v1'
      group by a.student_id,ps.offering_id,sq.question_id
      """,
      student, offering
    );
  }

  @Transactional
  public Map<String, Object> claim(UUID id, UUID guest, UUID student) {
    var s = owned(id, null, guest, true);
    require(s.get("status").equals("SUBMITTED"), "Finish the sample first");
    require(
      s.get("claimed_by") == null || s.get("claimed_by").equals(student),
      "Sample already belongs to another account"
    );
    db.update(
      "update practice_sessions set claimed_by=? where session_id=?",
      student,
      id
    );
    return read(id, student, guest);
  }
}
