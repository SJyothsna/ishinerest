package com.istudy.identity;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students/me")
public class ArchivedCourseHistoryController {

  private final JdbcTemplate db;
  private final CurrentStudent current;

  public ArchivedCourseHistoryController(JdbcTemplate db, CurrentStudent current) {
    this.db = db;
    this.current = current;
  }

  @GetMapping("/archived-sessions")
  public List<Map<String, Object>> archivedSessions() {
    return db.queryForList(
      """
      select ps.session_id as "sessionId",ps.session_kind as "kind",ps.submitted_at as "submittedAt",
        sub.name as "subjectName",o.track_code as "trackCode",c.title as "chapterName",
        ps.selected_count as "questionCount"
      from practice_sessions ps
      join subject_offerings o on o.offering_id=ps.offering_id
      join subjects sub on sub.subject_id=o.subject_id
      left join chapters c on c.chapter_id=ps.chapter_id
      where ps.student_id=? and ps.status='SUBMITTED' and ps.result_payload is not null
        and o.status='ARCHIVED'
      order by ps.submitted_at desc nulls last,ps.session_id
      limit 50
      """,
      current.id()
    );
  }
}
