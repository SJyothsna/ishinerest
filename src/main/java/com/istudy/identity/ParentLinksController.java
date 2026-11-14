package com.istudy.identity;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/parent-links")
public class ParentLinksController {

  private final JdbcTemplate db;
  private final CurrentStudent current;
  private final com.istudy.learning.InsightsController insights;

  public ParentLinksController(
    JdbcTemplate db,
    CurrentStudent current,
    com.istudy.learning.InsightsController insights
  ) {
    this.db = db;
    this.current = current;
    this.insights = insights;
  }

  private void verified(UUID student) {
    if (
      !Boolean.TRUE.equals(
        db.queryForObject(
          "select exists(select 1 from parent_student_links where parent_user_id=? and student_id=? and status='VERIFIED')",
          Boolean.class,
          current.id(),
          student
        )
      )
    ) throw new ResponseStatusException(
      HttpStatus.NOT_FOUND,
      "Student connection not found"
    );
  }

  @GetMapping("/students/{studentId}/subjects")
  public List<Map<String, Object>> subjects(@PathVariable UUID studentId) {
    verified(studentId);
    return db.queryForList(
      "select o.offering_id as \"offeringId\",s.name as \"subjectName\",o.track_code as \"trackCode\" from student_subjects ss join subject_offerings o on o.offering_id=ss.offering_id join subjects s on s.subject_id=o.subject_id where ss.student_id=? and ss.status='ENROLLED' and o.status='PUBLISHED'",
      studentId
    );
  }

  @GetMapping("/students/{studentId}/progress")
  public Map<String, Object> progress(
    @PathVariable UUID studentId,
    @RequestParam UUID offeringId
  ) {
    verified(studentId);
    return insights.progressFor(studentId, offeringId, null);
  }

  @GetMapping
  public List<Map<String, Object>> links() {
    return db.queryForList(
      "select l.parent_user_id as \"parentUserId\",l.student_id as \"studentId\",l.status,u.display_name as \"studentName\",u.email as \"studentEmail\",p.display_name as \"parentName\" from parent_student_links l join app_users u on u.user_id=l.student_id join app_users p on p.user_id=l.parent_user_id where l.parent_user_id=? or l.student_id=?",
      current.id(),
      current.id()
    );
  }

  public record Request(
    @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Email String studentEmail
  ) {}

  @PostMapping
  @Transactional
  public void request(@jakarta.validation.Valid @RequestBody Request request) {
    UUID parent = current.id();
    if (
      !"PARENT".equals(
          db.queryForObject(
            "select account_role from app_users where user_id=?",
            String.class,
            parent
          )
        )
    ) throw new ResponseStatusException(
      HttpStatus.FORBIDDEN,
      "Use a parent account to request a connection"
    );
    var ids = db.queryForList(
      "select u.user_id from app_users u join student_profiles s on s.student_id=u.user_id where lower(u.email)=? and u.status='ACTIVE'",
      UUID.class,
      request.studentEmail().trim().toLowerCase(Locale.ROOT)
    );
    if (ids.isEmpty()) throw new ResponseStatusException(
      HttpStatus.NOT_FOUND,
      "No student account was found"
    );
    UUID student = ids.get(0);
    if (
      db.update(
        "update parent_student_links set status='PENDING',verified_at=null where parent_user_id=? and student_id=? and status='REVOKED'",
        parent,
        student
      ) ==
      0
    ) {
      if (
        db.queryForObject(
          "select count(*) from parent_student_links where parent_user_id=? and student_id=?",
          Integer.class,
          parent,
          student
        ) >
        0
      ) throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "A connection already exists"
      );
      db.update(
        "insert into parent_student_links(parent_user_id,student_id) values(?,?)",
        parent,
        student
      );
    }
  }

  @PutMapping("/{parentId}/approval")
  public void approve(@PathVariable UUID parentId) {
    if (
      db.update(
        "update parent_student_links set status='VERIFIED',verified_at=current_timestamp where parent_user_id=? and student_id=? and status='PENDING'",
        parentId,
        current.id()
      ) ==
      0
    ) throw new ResponseStatusException(
      HttpStatus.NOT_FOUND,
      "Request not found"
    );
  }

  @DeleteMapping("/{parentId}/{studentId}")
  public void revoke(
    @PathVariable UUID parentId,
    @PathVariable UUID studentId
  ) {
    UUID actor = current.id();
    if (
      !actor.equals(parentId) && !actor.equals(studentId)
    ) throw new ResponseStatusException(
      HttpStatus.NOT_FOUND,
      "Connection not found"
    );
    db.update(
      "update parent_student_links set status='REVOKED',verified_at=null where parent_user_id=? and student_id=?",
      parentId,
      studentId
    );
  }
}
