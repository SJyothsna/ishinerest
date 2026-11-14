package com.istudy.learning;

import com.istudy.identity.CurrentStudent;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/students/me")
public class InsightsController {

  private final JdbcTemplate db;
  private final CurrentStudent current;
  private final LearningProfileService profiles;

  public InsightsController(JdbcTemplate db, CurrentStudent current) {
    this.db = db;
    this.current = current;
    this.profiles = new LearningProfileService(db);
  }

  @GetMapping("/progress")
  public Map<String, Object> progress(
    @RequestParam UUID offeringId,
    @RequestParam(required = false) UUID chapterId
  ) {
    return progressFor(current.id(), offeringId, chapterId);
  }

  public Map<String, Object> progressFor(
    UUID student,
    UUID offeringId,
    UUID chapterId
  ) {
    var eligible = db.queryForList(
      "select distinct q.question_id from questions q join question_placements p on p.question_id=q.question_id join sections s on s.section_id=p.section_id join chapters c on c.chapter_id=s.chapter_id where c.offering_id=? and (?::uuid is null or c.chapter_id=?::uuid) and q.approval_status='PUBLISHED' and q.visibility='GLOBAL' and p.active and c.status='ACTIVE' and s.status='ACTIVE'",
      UUID.class,
      offeringId,
      chapterId,
      chapterId
    );
    // No multiplication when a canonical question has several placements.
    var evidence = db.queryForList(
      "select sq.question_id,coalesce(ck.is_correct,a.is_correct) as is_correct,a.grading_version from question_attempts a join session_questions sq on sq.session_question_id=a.session_question_id join practice_sessions ps on ps.session_id=sq.session_id left join session_answer_checks ck on ck.session_question_id=sq.session_question_id join question_placements p on p.placement_id=sq.placement_id join sections s on s.section_id=p.section_id join chapters c on c.chapter_id=s.chapter_id where a.student_id=? and ps.offering_id=? and (?::uuid is null or c.chapter_id=?::uuid)",
      student,
      offeringId,
      chapterId,
      chapterId
    );
    var eligibleIds = new HashSet<UUID>(eligible);
    var ids = new HashSet<UUID>();
    int attempts = 0, correct = 0;
    for (var e : evidence) if (eligibleIds.contains(e.get("question_id"))) {
      ids.add((UUID) e.get("question_id"));
      if (!"self-review-v1".equals(e.get("grading_version"))) {
        attempts++;
        if (Boolean.TRUE.equals(e.get("is_correct"))) correct++;
      }
    }
    var result = new LinkedHashMap<String, Object>();
    result.put("eligibleQuestionCount", eligible.size());
    result.put("distinctQuestionsAttempted", ids.size());
    result.put("totalAttempts", attempts);
    result.put("correctAttempts", correct);
    result.put(
      "coveragePercent",
      eligible.isEmpty()
        ? null
        : Math.round((ids.size() * 100f) / eligible.size())
    );
    result.put(
      "accuracyPercent",
      attempts == 0 ? null : Math.round((correct * 100f) / attempts)
    );
    result.put("calculatedAt", java.time.Instant.now());
    result.put("statsVersion", "v1");
    result.put(
      "questionBankVersion",
      com.istudy.identity.LocalAuthService.hash(
        eligible.stream().map(UUID::toString).sorted().toList().toString()
      ).substring(0, 12)
    );
    return result;
  }

  @GetMapping("/learning-profile")
  public LearningProfileService.Profile profile(
    @RequestParam UUID offeringId,
    @RequestParam(required = false) UUID chapterId
  ) {
    UUID student = current.id();
    var allowed = db.queryForObject(
      "select exists(select 1 from student_subjects ss join subject_offerings so on so.offering_id=ss.offering_id where ss.student_id=? and ss.offering_id=? and ss.status='ENROLLED' and so.status='PUBLISHED')",
      Boolean.class, student, offeringId
    );
    if (!Boolean.TRUE.equals(allowed)) throw new ResponseStatusException(
      HttpStatus.NOT_FOUND, "Enrolled course not found"
    );
    return profiles.profile(student, offeringId, chapterId);
  }

  @GetMapping("/recommendations")
  public Map<String, Object> recommendations(
    @RequestParam(required = false) UUID offeringId,
    @RequestParam(required = false) UUID chapterId
  ) {
    if (offeringId != null) {
      var profile = profile(offeringId, chapterId);
      return Map.of(
        "hasSufficientHistory", profile.hasSufficientHistory(),
        "recommendations", profile.recommendations(),
        "statsVersion", "v2"
      );
    }
    UUID student = current.id();
    var courses = db.queryForList(
      """
      select ss.offering_id from student_subjects ss
      join subject_offerings so on so.offering_id=ss.offering_id
      where ss.student_id=? and ss.status='ENROLLED' and so.status='PUBLISHED'
      order by ss.last_activity_at desc nulls last,ss.enrolled_at desc
      """,
      UUID.class, student
    );
    var available = courses.stream().map(course -> profiles.profile(student, course, chapterId)).toList();
    return Map.of(
      "hasSufficientHistory", available.stream().anyMatch(LearningProfileService.Profile::hasSufficientHistory),
      "recommendations", available.stream().flatMap(p -> p.recommendations().stream()).limit(3).toList(),
      "statsVersion", "v2"
    );
  }

  @GetMapping("/resumable-sessions")
  public Map<String, Object> resume() {
    return Map.of(
      "sessions",
      db.queryForList(
        "select ps.session_id as \"sessionId\",ps.offering_id as \"offeringId\",ps.chapter_id as \"chapterId\",ps.session_kind as \"kind\",ps.selected_count as \"questionCount\",(select count(*) from session_answer_drafts d join session_questions sq on sq.session_question_id=d.session_question_id where sq.session_id=ps.session_id) as \"answeredCount\" from practice_sessions ps where ps.student_id=? and ps.status='IN_PROGRESS' and (ps.expires_at is null or ps.expires_at>current_timestamp) order by ps.created_at desc",
        current.id()
      )
    );
  }
}
