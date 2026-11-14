package com.istudy.learning;

import com.istudy.identity.CurrentStudent;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
public class SubjectOverviewController {

  private final JdbcTemplate db;
  private final CurrentStudent current;

  public SubjectOverviewController(JdbcTemplate db, CurrentStudent current) {
    this.db = db;
    this.current = current;
  }

  @GetMapping("/api/v1/subjects/{offeringId}/overview")
  public Map<String, Object> overview(@PathVariable UUID offeringId) {
    var counts = new HashMap<UUID, Integer>();
    for (var row : db.queryForList(
      "select c.chapter_id,count(distinct q.question_id) as total from chapters c join sections s on s.chapter_id=c.chapter_id join question_placements p on p.section_id=s.section_id join questions q on q.question_id=p.question_id where c.offering_id=? and c.status='ACTIVE' and s.status='ACTIVE' and p.active and q.approval_status='PUBLISHED' and q.visibility='GLOBAL' group by c.chapter_id",
      offeringId
    )) counts.put(
      (UUID) row.get("chapter_id"),
      ((Number) row.get("total")).intValue()
    );
    var attempted = new HashMap<UUID, Integer>();
    for (var row : db.queryForList(
      "select c.chapter_id,count(distinct sq.question_id) as attempted from question_attempts a join session_questions sq on sq.session_question_id=a.session_question_id join practice_sessions ps on ps.session_id=sq.session_id join question_placements p on p.placement_id=sq.placement_id join sections s on s.section_id=p.section_id join chapters c on c.chapter_id=s.chapter_id join questions q on q.question_id=sq.question_id where a.student_id=? and ps.offering_id=? and q.approval_status='PUBLISHED' and q.visibility='GLOBAL' and exists(select 1 from question_placements available join sections topic on topic.section_id=available.section_id where available.question_id=sq.question_id and available.active and topic.chapter_id=c.chapter_id and topic.status='ACTIVE') group by c.chapter_id",
      current.id(),
      offeringId
    )) attempted.put(
      (UUID) row.get("chapter_id"),
      ((Number) row.get("attempted")).intValue()
    );
    var chapters = db.queryForList(
      "select chapter_id as \"chapterId\",title as \"chapterName\",code from chapters where offering_id=? and status='ACTIVE' order by sort_order,chapter_id",
      offeringId
    );
    for (var c : chapters) {
      UUID id = (UUID) c.get("chapterId");
      int total = counts.getOrDefault(id, 0), seen = attempted.getOrDefault(
        id,
        0
      );
      c.put("eligibleQuestionCount", total);
      c.put("distinctQuestionsAttempted", seen);
      c.put(
        "coveragePercent",
        total > 0 && seen > 0 ? Math.round((seen * 100f) / total) : null
      );
    }
    return Map.of("chapters", chapters);
  }
}
