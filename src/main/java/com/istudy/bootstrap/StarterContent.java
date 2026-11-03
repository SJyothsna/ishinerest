package com.istudy.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Explicit local content reset. Accounts and historical session evidence are retained. */
@Component
@Profile("starter-content")
@Order(10)
public class StarterContent implements ApplicationRunner {

  private final JdbcTemplate db;
  private final ObjectMapper json;

  public StarterContent(JdbcTemplate db, ObjectMapper json) {
    this.db = db;
    this.json = json;
  }

  public static UUID id(String value) {
    return UUID.nameUUIDFromBytes(
      ("istudy-ireland-starter-v1:" + value).getBytes(StandardCharsets.UTF_8)
    );
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) throws Exception {
    var course = json.readTree(
      new ClassPathResource(
        "starter/ireland-leaving-cert-maths.json"
      ).getInputStream()
    );
    String key = "ireland-leaving-cert-maths-v1";
    if (
      db.queryForObject(
        "select count(*) from starter_content_runs where seed_key=?",
        Integer.class,
        key
      ) >
      0
    ) {
      refreshStarterQuestions(course);
      return;
    }
    UUID curriculum = id("curriculum"), level = id("level"), subject = id(
      "subject"
    ), offering = id("offering");
    db.update(
      "update student_subjects set status='ARCHIVED' where status='ENROLLED'"
    );
    db.update(
      "update subject_offerings set status='ARCHIVED' where status<>'ARCHIVED'"
    );
    db.update("update curricula set status='INACTIVE'");
    db.update("update subjects set status='INACTIVE'");
    db.update(
      "update content_jobs set status='COMPLETED',completed_at=current_timestamp where status in ('QUEUED','RUNNING','REVIEW')"
    );
    db.update(
      "insert into curricula(curriculum_id,code,name,country_code) values(?,'IE','Ireland','IE')",
      curriculum
    );
    db.update(
      "insert into academic_levels(level_id,code,name) values(?,'LEAVING-CERT','Leaving Certificate')",
      level
    );
    db.update(
      "insert into subjects(subject_id,code,name) values(?,'MATHEMATICS','Mathematics')",
      subject
    );
    db.update(
      "insert into subject_offerings(offering_id,curriculum_id,level_id,subject_id,syllabus_version,status,is_starter) values(?,?,?,?,'starter-v1','PUBLISHED',true)",
      offering,
      curriculum,
      level,
      subject
    );
    int order = 0;
    for (var chapter : course.path("chapters")) {
      UUID chapterId = id("chapter:" + chapter.path("code").asText());
      db.update(
        "insert into chapters(chapter_id,offering_id,code,title,description,sort_order) values(?,?,?,?,?,?)",
        chapterId,
        offering,
        chapter.path("code").asText(),
        chapter.path("title").asText(),
        chapter.path("description").asText(),
        order++
      );
      db.update(
        "insert into study_notes(offering_id,chapter_id,note_type,title,content_text,color,is_starter) values(?,?,'GENERAL',?,?,'#EDF3FF',true)",
        offering,
        chapterId,
        chapter.path("title").asText() + " · Starter notes",
        chapter.path("note").asText()
      );
      for (var sourceCode : chapter.path("sourceChapterCodes")) {
        for (UUID occurrence : db.queryForList(
          "select distinct pc.paper_question_id from past_paper_question_chapters pc join chapters c on c.chapter_id=pc.chapter_id where c.code=?",
          UUID.class,
          sourceCode.asText()
        )) {
          if (
            db.queryForObject(
              "select count(*) from past_paper_question_chapters where paper_question_id=? and chapter_id=?",
              Integer.class,
              occurrence,
              chapterId
            ) ==
            0
          ) db.update(
            "insert into past_paper_question_chapters(paper_question_id,chapter_id) values(?,?)",
            occurrence,
            chapterId
          );
          db.update(
            "update past_papers set exam_level='Higher Level' where paper_id=(select paper_id from past_paper_questions where paper_question_id=?)",
            occurrence
          );
        }
      }
      int topicOrder = 0;
      for (var topic : chapter.path("topics")) {
        UUID section = id("topic:" + topic.path("code").asText());
        db.update(
          "insert into sections(section_id,chapter_id,code,title,sort_order) values(?,?,?,?,?)",
          section,
          chapterId,
          topic.path("code").asText(),
          topic.path("title").asText(),
          topicOrder++
        );
        for (var q : topic.path("questions")) {
          UUID question = id("question:" + q.path("externalId").asText());
          String type = q.path("type").asText().equals("single")
            ? "SINGLE_CHOICE"
            : "TEXT";
          db.update(
            "insert into questions(question_id,question_text,correct_answer,question_type,explanation,difficulty_level,approval_status,tags) values(?,?,?,?,?,?,'PUBLISHED','[\"starter\"]')",
            question,
            q.path("prompt").asText(),
            q.path("correctAnswer").asText(),
            type,
            q.path("explanation").asText(),
            q.path("difficulty").asText()
          );
          db.update(
            "insert into question_revisions(question_id,revision_number,content_snapshot) values(?,1,?)",
            question,
            q.toString()
          );
          db.update(
            "insert into question_placements(question_id,section_id,usage_type,question_set,is_starter) values(?,?,'BOTH',1,true)",
            question,
            section
          );
          db.update(
            "insert into question_sources(question_id,source_note) values(?,'Original illustrative starter question; not sourced from a textbook or official exam.')",
            question
          );
        }
      }
    }
    db.update(
      "insert into past_paper_offerings(paper_id,offering_id) select paper_id,? from past_papers where approval_status='PUBLISHED' and (paper_code like 'LC01H:%' or paper_code like 'LC01O:%')",
      offering
    );
    db.update(
      "update past_papers set approval_status='ARCHIVED' where approval_status='PUBLISHED' and not exists(select 1 from past_paper_offerings po where po.paper_id=past_papers.paper_id and po.offering_id=?) and not exists(select 1 from past_paper_questions pq join past_paper_question_chapters pc on pc.paper_question_id=pq.paper_question_id join chapters c on c.chapter_id=pc.chapter_id where pq.paper_id=past_papers.paper_id and c.offering_id=?)",
      offering,
      offering
    );
    db.update(
      "update questions set approval_status='ARCHIVED' where approval_status='PUBLISHED' and not exists(select 1 from question_placements p join sections s on s.section_id=p.section_id join chapters c on c.chapter_id=s.chapter_id where p.question_id=questions.question_id and c.offering_id=?)",
      offering
    );
    db.update(
      "insert into student_subjects(student_id,offering_id) select student_id,? from student_profiles",
      offering
    );
    db.update("insert into starter_content_runs(seed_key) values(?)", key);
  }

  /** Revise only existing active starter questions; historical sessions retain their frozen revisions. */
  private void refreshStarterQuestions(
    com.fasterxml.jackson.databind.JsonNode course
  ) throws Exception {
    for (var chapter : course.path("chapters")) for (var topic : chapter.path(
      "topics"
    )) for (var question : topic.path("questions")) {
      UUID questionId = id("question:" + question.path("externalId").asText());
      var rows = db.queryForList(
        "select q.current_revision,r.content_snapshot from questions q join question_revisions r on r.question_id=q.question_id and r.revision_number=q.current_revision where q.question_id=? and q.approval_status='PUBLISHED' and exists(select 1 from question_placements p join sections s on s.section_id=p.section_id join chapters c on c.chapter_id=s.chapter_id join subject_offerings o on o.offering_id=c.offering_id where p.question_id=q.question_id and p.active and p.is_starter and c.status='ACTIVE' and s.status='ACTIVE' and o.is_starter and o.status='PUBLISHED') for update",
        questionId
      );
      if (rows.isEmpty()) continue;
      var row = rows.get(0);
      if (
        json.readTree(row.get("content_snapshot").toString()).equals(question)
      ) continue;
      int revision = ((Number) row.get("current_revision")).intValue() + 1;
      db.update(
        "insert into question_revisions(question_id,revision_number,content_snapshot) values(?,?,?)",
        questionId,
        revision,
        question.toString()
      );
      db.update(
        "update questions set question_type='SINGLE_CHOICE',question_text=?,correct_answer=?,explanation=?,current_revision=?,updated_at=current_timestamp where question_id=?",
        question.path("prompt").asText(),
        question.path("correctAnswer").asText(),
        question.path("explanation").asText(),
        revision,
        questionId
      );
    }
  }
}
