package com.istudy.resources;

import com.istudy.identity.CurrentStudent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ResourceController {

  private final JdbcTemplate db;
  private final CurrentStudent current;

  public ResourceController(JdbcTemplate db, CurrentStudent current) {
    this.db = db;
    this.current = current;
  }

  @GetMapping("/videos")
  public List<Map<String, Object>> videos(
    @RequestParam UUID offeringId,
    @RequestParam(required = false) UUID chapterId
  ) {
    return db.queryForList(
      "select distinct v.video_id as \"videoId\",v.youtube_video_id as \"youtubeVideoId\",v.title,v.duration_seconds as \"durationSeconds\",c.chapter_id as \"chapterId\",c.title as \"chapterName\",s.section_id as \"sectionId\",s.title as \"sectionName\" from learning_videos v join video_sections vs on vs.video_id=v.video_id join sections s on s.section_id=vs.section_id join chapters c on c.chapter_id=s.chapter_id where v.status='ACTIVE' and c.offering_id=? and (?::uuid is null or c.chapter_id=?::uuid)",
      offeringId,
      chapterId,
      chapterId
    );
  }

  @GetMapping("/past-papers")
  public List<Map<String, Object>> papers(
    @RequestParam UUID offeringId,
    @RequestParam(required = false) Integer year
  ) {
    return db.queryForList(
      "select p.paper_id as \"paperId\",p.exam_year as \"year\",d.title as \"title\",p.exam_session as \"examSession\",p.exam_level as \"examLevel\",d.asset_id as \"assetId\" from past_papers p join past_paper_offerings po on po.paper_id=p.paper_id join content_documents d on d.document_id=p.document_id where po.offering_id=? and (?::integer is null or p.exam_year=?::integer) and p.approval_status='PUBLISHED' and d.licence_status='PERMITTED' order by p.exam_year desc,p.paper_code",
      offeringId,
      year,
      year
    );
  }

  @GetMapping("/past-papers/{id}/marking-scheme")
  public Map<String, Object> marking(@PathVariable UUID id) {
    var rows = db.queryForList(
      "select d.asset_id as \"assetId\" from past_paper_marking_schemes m join content_documents d on d.document_id=m.document_id join past_papers p on p.paper_id=m.paper_id where p.paper_id=? and p.approval_status='PUBLISHED' and d.licence_status='PERMITTED'",
      id
    );
    return rows.isEmpty() ? Map.of() : rows.get(0);
  }

  @GetMapping("/chapter-past-questions")
  public List<Map<String, Object>> chapterQuestions(
    @RequestParam UUID chapterId
  ) {
    return db.queryForList(
      "select distinct pq.paper_question_id as \"occurrenceId\",p.exam_year as \"year\",pq.extracted_text as \"paperName\",pq.question_number as \"questionNumber\",p.exam_session as \"paperType\" from past_paper_questions pq join past_papers p on p.paper_id=pq.paper_id where exists(select 1 from past_paper_question_chapters pc where pc.paper_question_id=pq.paper_question_id and pc.chapter_id=?) and pq.review_status='APPROVED' and p.approval_status='PUBLISHED' order by p.exam_year desc,pq.question_number",
      chapterId
    );
  }

  @GetMapping("/past-questions/{id}/assets")
  public List<Map<String, Object>> questionAssets(@PathVariable UUID id) {
    return db.queryForList(
      "select a.asset_id as \"assetId\",a.asset_kind as \"kind\" from past_paper_question_assets a join past_paper_questions q on q.paper_question_id=a.paper_question_id join past_papers p on p.paper_id=q.paper_id where q.paper_question_id=? and q.review_status='APPROVED' and p.approval_status='PUBLISHED' order by a.sort_order",
      id
    );
  }

  @GetMapping("/past-questions")
  public List<Map<String, Object>> pastQuestions(@RequestParam UUID chapterId) {
    return db.queryForList(
      "select distinct pq.paper_question_id as \"paperQuestionId\",pq.question_number as \"questionNumber\",pq.part_label as \"partLabel\",pq.extracted_text as \"prompt\",p.paper_code as \"paperName\",p.exam_year as \"year\" from past_paper_questions pq join past_papers p on p.paper_id=pq.paper_id where exists(select 1 from past_paper_question_chapters pc where pc.paper_question_id=pq.paper_question_id and pc.chapter_id=?) and pq.review_status='APPROVED' and p.approval_status='PUBLISHED' order by p.exam_year desc,pq.question_number",
      chapterId
    );
  }

  public record Feedback(
    @NotBlank @Size(max = 200) String name,
    @Email @NotBlank String email,
    @NotBlank @Size(max = 30) String type,
    @NotBlank @Size(max = 300) String subject,
    @NotBlank @Size(max = 10000) String message
  ) {}

  @PostMapping("/feedback")
  public Map<String, Object> feedback(@Valid @RequestBody Feedback f) {
    db.update(
      "insert into feedback_messages(name,email,kind,subject,message) values(?,?,?,?,?)",
      f.name(),
      f.email(),
      f.type(),
      f.subject(),
      f.message()
    );
    return Map.of("message", "Your message has been received.");
  }

  @PutMapping("/questions/{questionId}/flag")
  public void flag(@PathVariable UUID questionId) {
    UUID student = current.id();
    if (
      db.queryForObject(
        "select count(*) from student_question_flags where student_id=? and question_id=?",
        Integer.class,
        student,
        questionId
      ) ==
      0
    ) db.update(
      "insert into student_question_flags(student_id,question_id) values(?,?)",
      student,
      questionId
    );
  }

  @DeleteMapping("/questions/{questionId}/flag")
  public void unflag(@PathVariable UUID questionId) {
    db.update(
      "delete from student_question_flags where student_id=? and question_id=?",
      current.id(),
      questionId
    );
  }
}
