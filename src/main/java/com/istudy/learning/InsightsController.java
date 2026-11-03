package com.istudy.learning;

import com.istudy.identity.CurrentStudent;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/students/me")
public class InsightsController {

  private final JdbcTemplate db;
  private final CurrentStudent current;

  public InsightsController(JdbcTemplate db, CurrentStudent current) {
    this.db = db;
    this.current = current;
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
      "select sq.question_id,coalesce(ck.is_correct,a.is_correct) as is_correct,a.grading_version from question_attempts a join session_questions sq on sq.session_question_id=a.session_question_id left join session_answer_checks ck on ck.session_question_id=sq.session_question_id join question_placements p on p.placement_id=sq.placement_id join sections s on s.section_id=p.section_id join chapters c on c.chapter_id=s.chapter_id where a.student_id=? and c.offering_id=? and (?::uuid is null or c.chapter_id=?::uuid)",
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

  @GetMapping("/recommendations")
  public Map<String, Object> recommendations(
    @RequestParam(required = false) UUID offeringId,
    @RequestParam(required = false) UUID chapterId
  ) {
    // Thresholds are conservative provisional product parameters, not a knowledge-level label.
    var rows = db.queryForList(
      """
      select s.section_id as "sectionId",s.title,c.chapter_id as "chapterId",c.offering_id as "offeringId",
      count(distinct sq.question_id) as diversity,count(*) as attempts,
      sum(case when coalesce(ck.is_correct,a.is_correct) then 1 else 0 end) as correct,count(distinct sq.session_id) as sessions
      from question_attempts a join session_questions sq on sq.session_question_id=a.session_question_id left join session_answer_checks ck on ck.session_question_id=sq.session_question_id
      join question_placements p on p.placement_id=sq.placement_id join sections s on s.section_id=p.section_id join chapters c on c.chapter_id=s.chapter_id
      where a.grading_version<>'self-review-v1' and a.student_id=? and (?::uuid is null or c.offering_id=?::uuid) and (?::uuid is null or c.chapter_id=?::uuid)
      group by s.section_id,s.title,c.chapter_id,c.offering_id
      having count(distinct sq.question_id)>=3 and count(*)>=6 and count(distinct sq.session_id)>=2
      order by sum(case when coalesce(ck.is_correct,a.is_correct) then 1.0 else 0 end)/count(*) asc
      """,
      current.id(),
      offeringId,
      offeringId,
      chapterId,
      chapterId
    );
    if (rows.isEmpty()) return Map.of(
      "hasSufficientHistory",
      false,
      "recommendations",
      List.of(),
      "statsVersion",
      "v1"
    );
    var first = rows.get(0);
    double accuracy =
      ((Number) first.get("correct")).doubleValue() /
      ((Number) first.get("attempts")).doubleValue();
    if (accuracy >= 0.8) return Map.of(
      "hasSufficientHistory",
      true,
      "recommendations",
      List.of(),
      "statsVersion",
      "v1"
    );
    return Map.of(
      "hasSufficientHistory",
      true,
      "recommendations",
      List.of(
        Map.of(
          "title",
          "Practise " + first.get("title"),
          "reason",
          "Your submitted answers suggest more practice could help with this topic.",
          "chapterId",
          first.get("chapterId"),
          "sectionId",
          first.get("sectionId"),
          "offeringId",
          first.get("offeringId"),
          "evidenceCount",
          first.get("attempts")
        )
      ),
      "statsVersion",
      "v1"
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
