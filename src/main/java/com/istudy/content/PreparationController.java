package com.istudy.content;

import com.istudy.identity.CurrentStudent;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/content/jobs")
public class PreparationController {

  private final JdbcTemplate db;
  private final CurrentStudent current;

  public PreparationController(JdbcTemplate db, CurrentStudent current) {
    this.db = db;
    this.current = current;
  }

  @GetMapping
  public List<Map<String, Object>> requests() {
    if (
      !Boolean.TRUE.equals(
        db.queryForObject(
          "select content_admin from app_users where user_id=?",
          Boolean.class,
          current.id()
        )
      )
    ) throw new ResponseStatusException(
      HttpStatus.FORBIDDEN,
      "Content administration permission is required"
    );
    return db.queryForList(
      "select j.job_id as \"jobId\",j.chapter_id as \"chapterId\",c.title as \"chapterTitle\",j.status,j.error_code as \"errorCode\",j.created_at as \"createdAt\" from content_jobs j left join chapters c on c.chapter_id=j.chapter_id where exists(select 1 from subject_offerings o where o.offering_id=c.offering_id and o.status='PUBLISHED') and j.operation='GENERATE_QUESTIONS' order by j.created_at desc"
    );
  }
}
