package com.istudy.catalogue;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalogue")
public class CatalogueController {

  private final JdbcTemplate db;

  public CatalogueController(JdbcTemplate db) {
    this.db = db;
  }

  @GetMapping("/offerings")
  public List<Map<String, Object>> offerings() {
    return db.queryForList(
      """
      select o.offering_id as "offeringId",s.subject_id as "subjectId",s.name as "subjectName",
      l.level_id as "levelId",l.name as "levelName",c.curriculum_id as "curriculumId",c.name as "curriculumName",
      o.track_code as "trackCode",o.syllabus_version as "syllabusVersion",o.is_starter as "isStarter"
      from subject_offerings o join subjects s on s.subject_id=o.subject_id join academic_levels l on l.level_id=o.level_id join curricula c on c.curriculum_id=o.curriculum_id
      where o.status='PUBLISHED' and s.status='ACTIVE' and c.status='ACTIVE' order by l.sort_order,s.name,o.track_code
      """
    );
  }

  @GetMapping("/offerings/{id}/sample-options")
  public Map<String, Object> sampleOptions(@PathVariable UUID id) {
    String eligible =
      " from question_placements p join questions q on q.question_id=p.question_id join sections s on s.section_id=p.section_id join chapters c on c.chapter_id=s.chapter_id join subject_offerings o on o.offering_id=c.offering_id where c.offering_id=? and o.status='PUBLISHED' and c.status='ACTIVE' and s.status='ACTIVE' and p.active and q.approval_status='PUBLISHED' and q.visibility='GLOBAL' and q.question_type<>'WORKED_EXAMPLE'";
    var chapters = db.queryForList(
      "select c.chapter_id as \"chapterId\",c.title,count(distinct q.question_id) as \"questionCount\"" +
      eligible +
      " group by c.chapter_id,c.title,c.sort_order order by c.sort_order,c.chapter_id",
      id
    );
    return Map.of(
      "questionCount",
      db.queryForObject(
        "select count(distinct q.question_id)" + eligible,
        Integer.class,
        id
      ),
      "chapters",
      chapters
    );
  }

  @GetMapping("/offerings/{id}/preview")
  public Map<String, Object> preview(@PathVariable UUID id) {
    var topics = db.queryForList(
      "select c.title,c.code from chapters c join subject_offerings o on o.offering_id=c.offering_id where c.offering_id=? and c.status='ACTIVE' and o.status='PUBLISHED' order by c.sort_order limit 3",
      id
    );
    var papers = db.queryForList(
      "select p.exam_year as \"year\",p.exam_level as \"examLevel\",d.title from past_papers p join past_paper_offerings po on po.paper_id=p.paper_id join subject_offerings o on o.offering_id=po.offering_id join content_documents d on d.document_id=p.document_id where po.offering_id=? and o.status='PUBLISHED' and p.approval_status='PUBLISHED' and d.licence_status='PERMITTED' order by p.exam_year desc,p.paper_code limit 3",
      id
    );
    return Map.of("chapters", topics, "papers", papers);
  }

  @GetMapping("/offerings/{id}/chapters")
  public List<Map<String, Object>> chapters(@PathVariable UUID id) {
    return db.queryForList(
      "select c.chapter_id as \"chapterId\",c.code,c.title,c.description,c.sort_order as \"sortOrder\" from chapters c join subject_offerings o on o.offering_id=c.offering_id where c.offering_id=? and c.status='ACTIVE' and o.status='PUBLISHED' order by c.sort_order,c.chapter_id",
      id
    );
  }

  @GetMapping("/chapters/{id}/sections")
  public List<Map<String, Object>> sections(@PathVariable UUID id) {
    return db.queryForList(
      "select s.section_id as \"sectionId\",s.code,s.title,s.sort_order as \"sortOrder\" from sections s join chapters c on c.chapter_id=s.chapter_id join subject_offerings o on o.offering_id=c.offering_id where s.chapter_id=? and s.status='ACTIVE' and c.status='ACTIVE' and o.status='PUBLISHED' order by s.sort_order,s.section_id",
      id
    );
  }
}
