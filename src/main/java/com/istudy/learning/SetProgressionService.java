package com.istudy.learning;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SetProgressionService {

  private final JdbcTemplate db;
  private final int threshold;

  public SetProgressionService(
    JdbcTemplate db,
    @Value("${app.learning.set2-trigger-percent:80}") int threshold
  ) {
    this.db = db;
    this.threshold = threshold;
  }

  public Map<String, Object> status(UUID student, UUID offering, UUID chapter) {
    var totals = db.queryForMap(
      "select count(distinct q.question_id) as total from question_placements p join questions q on q.question_id=p.question_id join sections s on s.section_id=p.section_id join chapters c on c.chapter_id=s.chapter_id where c.offering_id=? and (?::uuid is null or c.chapter_id=?::uuid) and p.question_set=1 and p.active and q.approval_status='PUBLISHED' and q.visibility='GLOBAL'",
      offering,
      chapter,
      chapter
    );
    int total = ((Number) totals.get("total")).intValue();
    int seen = db.queryForObject(
      "select count(distinct sq.question_id) from session_questions sq join practice_sessions ps on ps.session_id=sq.session_id join question_placements p on p.placement_id=sq.placement_id join sections s on s.section_id=p.section_id where (ps.student_id=? or ps.claimed_by=?) and ps.offering_id=? and ps.session_kind in ('PRACTICE','TEST','INITIAL_ASSESSMENT','SAMPLE') and p.question_set=1 and (?::uuid is null or s.chapter_id=?::uuid)",
      Integer.class,
      student,
      student,
      offering,
      chapter,
      chapter
    );
    int available = db.queryForObject(
      "select count(distinct q.question_id) from question_placements p join questions q on q.question_id=p.question_id join sections s on s.section_id=p.section_id join chapters c on c.chapter_id=s.chapter_id where c.offering_id=? and (?::uuid is null or c.chapter_id=?::uuid) and p.question_set=2 and p.active and q.approval_status='PUBLISHED' and q.visibility='GLOBAL' and not exists(select 1 from session_questions sq join practice_sessions ps on ps.session_id=sq.session_id where (ps.student_id=? or ps.claimed_by=?) and ps.offering_id=c.offering_id and sq.question_id=q.question_id)",
      Integer.class,
      offering,
      chapter,
      chapter,
      student,
      student
    );
    var jobs = db.queryForList(
      "select j.status,j.error_code from content_jobs j join chapters c on c.chapter_id=j.chapter_id where c.offering_id=? and (?::uuid is null or c.chapter_id=?::uuid) and j.operation='GENERATE_QUESTIONS' order by j.created_at desc",
      offering,
      chapter,
      chapter
    );
    String state = available > 0
      ? "READY"
      : jobs.isEmpty()
        ? "NOT_NEEDED"
        : jobs.get(0).get("error_code") != null
          ? jobs.get(0).get("error_code").toString()
          : jobs.get(0).get("status").toString();
    return Map.of(
      "set1Total",
      total,
      "set1Seen",
      Math.min(total, seen),
      "set1ExposurePercent",
      total == 0 ? 0 : Math.min(100, Math.round((seen * 100f) / total)),
      "freshSet2Count",
      available,
      "preparationStatus",
      state,
      "triggerPercent",
      threshold
    );
  }

  public void prepare(UUID student, UUID offering, Collection<UUID> chapters) {
    // A learner can trigger a shared content batch, but their identity/history is never sent to AI.
    for (UUID chapter : new HashSet<>(chapters)) {
      var state = status(student, offering, chapter);
      if (
        ((Number) state.get("set1ExposurePercent")).intValue() < threshold ||
        ((Number) state.get("freshSet2Count")).intValue() >= 5
      ) continue;
      db.queryForObject(
        "select chapter_id from chapters where chapter_id=? for update",
        UUID.class,
        chapter
      );
      int published = db.queryForObject(
        "select count(distinct p.question_id) from question_placements p join sections s on s.section_id=p.section_id join questions q on q.question_id=p.question_id where s.chapter_id=? and p.question_set=2 and p.active and q.approval_status='PUBLISHED'",
        Integer.class,
        chapter
      );
      if (
        db.queryForObject(
          "select count(*) from content_jobs where chapter_id=? and operation='GENERATE_QUESTIONS' and status in ('QUEUED','RUNNING','REVIEW')",
          Integer.class,
          chapter
        ) >
        0
      ) continue;
      String key = chapter + ":set2:" + published;
      if (
        db.queryForObject(
          "select count(*) from content_jobs where generation_key=?",
          Integer.class,
          key
        ) >
        0
      ) continue;
      db.update(
        "insert into content_jobs(operation,status,generation_key,chapter_id,input_parameters,prompt_version,error_code) values('GENERATE_QUESTIONS','QUEUED',?,?,?,'set2-v1','WAITING_MANUAL_UPLOAD')",
        key,
        chapter,
        "{\"questionSet\":2,\"count\":12,\"challengeStep\":1}"
      );
    }
  }
}
