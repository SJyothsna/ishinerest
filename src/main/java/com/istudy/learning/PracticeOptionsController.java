package com.istudy.learning;

import com.istudy.identity.CurrentStudent;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/practice-options")
public class PracticeOptionsController {

  private final JdbcTemplate db;
  private final CurrentStudent current;
  private final SetProgressionService progression;

  public PracticeOptionsController(
    JdbcTemplate db,
    CurrentStudent current,
    SetProgressionService progression
  ) {
    this.db = db;
    this.current = current;
    this.progression = progression;
  }

  @GetMapping
  public Map<String, Object> options(
    @RequestParam UUID offeringId,
    @RequestParam(required = false) UUID chapterId
  ) {
    var rows = db.queryForList(
      "select distinct q.question_id,st.total_attempts,st.correct_attempts,case when f.question_id is null then false else true end as flagged from questions q join question_placements p on p.question_id=q.question_id join sections s on s.section_id=p.section_id join chapters c on c.chapter_id=s.chapter_id left join student_question_stats st on st.question_id=q.question_id and st.student_id=? left join student_question_flags f on f.question_id=q.question_id and f.student_id=? where c.offering_id=? and (?::uuid is null or c.chapter_id=?::uuid) and q.approval_status='PUBLISHED' and q.visibility='GLOBAL' and c.status='ACTIVE' and s.status='ACTIVE' and p.active",
      current.id(),
      current.id(),
      offeringId,
      chapterId,
      chapterId
    );
    int flagged = 0, mistakes = 0;
    for (var row : rows) {
      if (Boolean.TRUE.equals(row.get("flagged"))) flagged++;
      if (
        row.get("total_attempts") instanceof Number total &&
        row.get("correct_attempts") instanceof Number correct &&
        total.intValue() > correct.intValue()
      ) mistakes++;
    }
    return Map.of(
      "quickCount",
      rows.size(),
      "flaggedCount",
      flagged,
      "mistakeCount",
      mistakes,
      "progression",
      progression.status(current.id(), offeringId, chapterId)
    );
  }
}
