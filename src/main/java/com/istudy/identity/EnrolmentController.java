package com.istudy.identity;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/students/me/subjects")
public class EnrolmentController {

  private final JdbcTemplate db;
  private final CurrentStudent current;

  public EnrolmentController(JdbcTemplate db, CurrentStudent current) {
    this.db = db;
    this.current = current;
  }

  @GetMapping
  public List<Map<String, Object>> subjects() {
    return db.queryForList(
      "select o.offering_id as \"offeringId\",s.name as \"subjectName\",o.track_code as \"trackCode\",o.syllabus_version as \"syllabusVersion\",o.is_starter as \"isStarter\",c.name as \"curriculumName\",l.name as \"levelName\",ss.started_at as \"startedAt\" from student_subjects ss join subject_offerings o on o.offering_id=ss.offering_id join subjects s on s.subject_id=o.subject_id join curricula c on c.curriculum_id=o.curriculum_id join academic_levels l on l.level_id=o.level_id where ss.student_id=? and ss.status='ENROLLED' and o.status='PUBLISHED' and s.status='ACTIVE' and c.status='ACTIVE'",
      current.id()
    );
  }

  @PutMapping
  @Transactional
  public List<Map<String, Object>> enrol(@RequestBody List<UUID> offerings) {
    UUID id = current.id();
    if (
      db.queryForObject(
        "select count(*) from student_profiles where student_id=?",
        Integer.class,
        id
      ) ==
      0
    ) throw new ResponseStatusException(
      HttpStatus.FORBIDDEN,
      "Use a student account to choose subjects"
    );
    if (
      offerings == null || offerings.size() > 100
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Choose a valid subject list"
    );
    db.queryForObject(
      "select student_id from student_profiles where student_id=? for update",
      UUID.class,
      id
    );
    for (UUID offering : offerings) {
      if (
        !Boolean.TRUE.equals(
          db.queryForObject(
            "select exists(select 1 from subject_offerings where offering_id=? and status='PUBLISHED')",
            Boolean.class,
            offering
          )
        )
      ) throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Subject is not available"
      );
    }
    db.update(
      "update student_subjects set status='ARCHIVED' where student_id=?",
      id
    );
    for (UUID offering : new HashSet<>(offerings)) if (
      db.update(
        "update student_subjects set status='ENROLLED' where student_id=? and offering_id=?",
        id,
        offering
      ) ==
      0
    ) db.update(
      "insert into student_subjects(student_id,offering_id) values(?,?)",
      id,
      offering
    );
    return subjects();
  }
}
