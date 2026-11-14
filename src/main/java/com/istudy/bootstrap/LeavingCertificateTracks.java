package com.istudy.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Separates the existing Irish Mathematics catalogue without reattributing learner history. */
@Component
@Order(20)
public class LeavingCertificateTracks implements ApplicationRunner {

  private static final UUID PILOT_DOCUMENT = UUID.fromString(
    "4bafc0c3-bf46-40cf-99a0-98a24b9036f7"
  );

  private final JdbcTemplate db;
  private final ObjectMapper json;

  public LeavingCertificateTracks(JdbcTemplate db, ObjectMapper json) {
    this.db = db;
    this.json = json;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void run(ApplicationArguments args) throws Exception {
    var offerings = db.queryForList(
      """
      select o.offering_id from subject_offerings o
      join curricula c on c.curriculum_id=o.curriculum_id
      join academic_levels l on l.level_id=o.level_id
      join subjects s on s.subject_id=o.subject_id
      where c.country_code='IE' and l.code='LEAVING-CERT' and s.code='MATHEMATICS'
      and o.track_code='STANDARD' and o.status='PUBLISHED'
      order by o.offering_id for update
      """,
      UUID.class
    );
    for (UUID offering : offerings) split(offering);
  }

  private void split(UUID source) throws Exception {
    boolean starter = Boolean.TRUE.equals(db.queryForObject(
      "select is_starter from subject_offerings where offering_id=?",
      Boolean.class, source
    ));
    var placements = db.queryForList(
      """
      select p.*,r.content_snapshot from question_placements p
      join sections s on s.section_id=p.section_id
      join chapters c on c.chapter_id=s.chapter_id
      join questions q on q.question_id=p.question_id
      join question_revisions r on r.question_id=q.question_id and r.revision_number=q.current_revision
      where c.offering_id=? and c.status='ACTIVE' and s.status='ACTIVE'
      and p.active and q.approval_status='PUBLISHED' and q.visibility='GLOBAL'
      """,
      source
    );
    var byTrack = new LinkedHashMap<String, List<Map<String, Object>>>();
    byTrack.put("ORDINARY", new ArrayList<>());
    byTrack.put("HIGHER", new ArrayList<>());
    for (var placement : placements) {
      String applicability = json.readTree(placement.get("content_snapshot").toString())
        .path("examLevel").asText();
      for (var track : byTrack.entrySet()) {
        if (applicability.equals(track.getKey()) || applicability.equals("COMMON") ||
            (starter && Boolean.TRUE.equals(placement.get("is_starter")))) {
          track.getValue().add(placement);
        }
      }
    }
    // Imported catalogues without explicit level evidence stay available for content review.
    if (!starter && byTrack.values().stream().allMatch(List::isEmpty)) return;

    if (Boolean.TRUE.equals(db.queryForObject(
      """
      select exists(select 1 from subject_offerings o join subject_offerings src
        on src.curriculum_id=o.curriculum_id and src.level_id=o.level_id
        and src.subject_id=o.subject_id and src.syllabus_version=o.syllabus_version
        where src.offering_id=? and o.track_code in ('ORDINARY','HIGHER') and o.status<>'PUBLISHED')
      """,
      Boolean.class, source
    ))) return;
    for (var track : byTrack.entrySet()) {
      if (Boolean.TRUE.equals(db.queryForObject(
        """
        select exists(select 1 from subject_offerings o join subject_offerings src
          on src.curriculum_id=o.curriculum_id and src.level_id=o.level_id
          and src.subject_id=o.subject_id and src.syllabus_version=o.syllabus_version
          where src.offering_id=? and o.track_code=? and o.status='PUBLISHED')
        """,
        Boolean.class, source, track.getKey()
      ))) continue;
      UUID target = UUID.randomUUID();
      db.update(
        """
        insert into subject_offerings(offering_id,curriculum_id,level_id,subject_id,track_code,syllabus_version,status,effective_from,effective_to,is_starter)
        select ?,curriculum_id,level_id,subject_id,?,syllabus_version,'PUBLISHED',effective_from,effective_to,is_starter
        from subject_offerings where offering_id=?
        """,
        target, track.getKey(), source
      );
      copyCourse(source, target, track.getKey(), track.getValue());
    }
    db.update("update student_subjects set status='ARCHIVED' where offering_id=? and status='ENROLLED'", source);
    db.update("update subject_offerings set status='ARCHIVED' where offering_id=?", source);
  }

  private void copyCourse(UUID source, UUID target, String track, List<Map<String, Object>> placements) throws Exception {
    var usedSections = new HashSet<UUID>();
    for (var placement : placements) usedSections.add((UUID) placement.get("section_id"));
    var sections = db.queryForList(
      """
      select s.* from sections s join chapters c on c.chapter_id=s.chapter_id
      where c.offering_id=? and c.status='ACTIVE' and s.status='ACTIVE'
      """, source
    );
    var sectionsById = new HashMap<UUID, Map<String, Object>>();
    for (var section : sections) sectionsById.put((UUID) section.get("section_id"), section);
    for (UUID section : List.copyOf(usedSections)) {
      UUID parent = (UUID) sectionsById.get(section).get("parent_section_id");
      while (parent != null && sectionsById.containsKey(parent) && usedSections.add(parent)) {
        parent = (UUID) sectionsById.get(parent).get("parent_section_id");
      }
    }
    var usedChapters = new HashSet<UUID>();
    for (UUID section : usedSections) usedChapters.add((UUID) sectionsById.get(section).get("chapter_id"));
    String examLevel = track.equals("ORDINARY") ? "Ordinary Level" : "Higher Level";
    db.update(
      """
      insert into past_paper_offerings(paper_id,offering_id)
      select po.paper_id,? from past_paper_offerings po join past_papers p on p.paper_id=po.paper_id
      where po.offering_id=? and p.exam_level=?
      """, target, source, examLevel
    );
    usedChapters.addAll(db.queryForList(
      """
      select distinct c.chapter_id from chapters c
      join past_paper_question_chapters pc on pc.chapter_id=c.chapter_id
      join past_paper_questions pq on pq.paper_question_id=pc.paper_question_id
      join past_paper_offerings po on po.paper_id=pq.paper_id
      where c.offering_id=? and c.status='ACTIVE' and po.offering_id=?
      """, UUID.class, source, target
    ));

    var chapterIds = new HashMap<UUID, UUID>();
    for (UUID chapter : usedChapters) {
      UUID newChapter = UUID.randomUUID();
      chapterIds.put(chapter, newChapter);
      db.update(
        """
        insert into chapters(chapter_id,offering_id,code,title,description,sort_order)
        select ?,?,code,title,description,sort_order from chapters where chapter_id=?
        """, newChapter, target, chapter
      );
      db.update(
        """
        insert into past_paper_question_chapters(paper_question_id,chapter_id)
        select pc.paper_question_id,? from past_paper_question_chapters pc
        join past_paper_questions pq on pq.paper_question_id=pc.paper_question_id
        join past_paper_offerings po on po.paper_id=pq.paper_id
        where pc.chapter_id=? and po.offering_id=?
        """, newChapter, chapter, target
      );
    }
    var sectionIds = new HashMap<UUID, UUID>();
    for (UUID section : usedSections) {
      UUID newSection = UUID.randomUUID();
      sectionIds.put(section, newSection);
      db.update(
        """
        insert into sections(section_id,chapter_id,code,title,sort_order,selection_priority)
        select ?,?,code,title,sort_order,selection_priority from sections where section_id=?
        """,
        newSection, chapterIds.get(sectionsById.get(section).get("chapter_id")), section
      );
    }
    for (UUID section : usedSections) {
      UUID parent = sectionIds.get(sectionsById.get(section).get("parent_section_id"));
      if (parent != null) db.update("update sections set parent_section_id=? where section_id=?", parent, sectionIds.get(section));
    }
    // Starter notes are illustrative shared material for the two starter tracks.
    // Other notes remain with the original course until their applicability is reviewed.
    for (var chapter : chapterIds.entrySet()) db.update(
      """
      insert into study_notes(offering_id,chapter_id,note_type,title,content_text,color,is_starter)
      select ?,?,'GENERAL',title,content_text,color,true from study_notes
      where offering_id=? and chapter_id=? and note_type='GENERAL' and is_starter
        and section_id is null and attachment_asset_id is null
      """,
      target, chapter.getValue(), source, chapter.getKey()
    );
    var objectiveIds = new HashMap<UUID, UUID>();
    for (var section : sectionIds.entrySet()) {
      for (var objective : db.queryForList(
        "select objective_id,code,title,required from learning_objectives where section_id=?",
        section.getKey()
      )) {
        UUID copy = UUID.randomUUID();
        objectiveIds.put((UUID) objective.get("objective_id"), copy);
        db.update(
          "insert into learning_objectives(objective_id,section_id,code,title,required) values(?,?,?,?,?)",
          copy, section.getValue(), objective.get("code"), objective.get("title"), objective.get("required")
        );
      }
    }
    var questionIds = new HashMap<UUID, UUID>();
    for (var placement : placements) {
      UUID original = (UUID) placement.get("question_id");
      UUID copy = questionIds.computeIfAbsent(original, this::copyQuestion);
      db.update(
        """
        insert into question_placements(question_id,section_id,usage_type,question_set,difficulty_override,is_starter,objective_id,family_code)
        values(?,?,?,?,?,?,?,?)
        """,
        copy, sectionIds.get(placement.get("section_id")), placement.get("usage_type"),
        placement.get("question_set"), placement.get("difficulty_override"), placement.get("is_starter"),
        objectiveIds.get(placement.get("objective_id")), placement.get("family_code")
      );
    }
    mapLinearEquationsPilot(target, questionIds);
  }

  private UUID copyQuestion(UUID original) {
    UUID copy = UUID.randomUUID();
    db.update(
      """
      insert into questions(question_id,question_text,option_a,option_b,option_c,option_d,option_e,option_f,
        correct_answer,correct_answers,question_type,explanation,hint,difficulty_level,tags,
        question_image_asset_id,creator_id,visibility,approval_status,created_at,updated_at)
      select ?,question_text,option_a,option_b,option_c,option_d,option_e,option_f,
        correct_answer,correct_answers,question_type,explanation,hint,difficulty_level,tags,
        question_image_asset_id,creator_id,visibility,approval_status,created_at,updated_at
      from questions where question_id=?
      """, copy, original
    );
    db.update(
      """
      insert into question_revisions(question_id,revision_number,content_snapshot,reviewed_by,created_at)
      select ?,1,r.content_snapshot,r.reviewed_by,r.created_at from question_revisions r
      join questions q on q.question_id=r.question_id and q.current_revision=r.revision_number
      where q.question_id=?
      """, copy, original
    );
    db.update(
      """
      insert into question_sources(question_id,document_id,chunk_id,page_number,generation_job_id,source_note)
      select ?,document_id,chunk_id,page_number,generation_job_id,source_note from question_sources where question_id=?
      """, copy, original
    );
    return copy;
  }

  private void mapLinearEquationsPilot(UUID offering, Map<UUID, UUID> questionIds) throws Exception {
    try (var stream = new ClassPathResource("coverage/linear-equations-pilot.json").getInputStream()) {
      var plan = json.readTree(stream);
      for (var topic : plan.path("topics")) {
        String code = topic.path("code").asText();
        var mappedQuestions = db.queryForList(
          "select question_id from external_question_keys where document_id=? and question_set=1 and external_id like ?",
          UUID.class, PILOT_DOCUMENT, code + ".Q%"
        ).stream().map(questionIds::get).filter(Objects::nonNull).distinct().toList();
        if (mappedQuestions.isEmpty()) continue;
        var sections = db.queryForList(
          "select s.section_id from sections s join chapters c on c.chapter_id=s.chapter_id where c.offering_id=? and c.code=? and s.code=? and not exists (select 1 from learning_objectives o where o.section_id=s.section_id)",
          UUID.class, offering, plan.path("chapterCode").asText(), code
        );
        for (UUID section : sections) for (var objective : topic.path("objectives")) {
          UUID objectiveId = UUID.randomUUID();
          db.update(
            "insert into learning_objectives(objective_id,section_id,code,title,required) values(?,?,?,?,true)",
            objectiveId, section, objective.path("code").asText(), objective.path("title").asText()
          );
          if (objective.hasNonNull("familyCode")) db.batchUpdate(
            "update question_placements set objective_id=?,family_code=? where question_id=? and section_id=? and question_set=1",
            mappedQuestions.stream().map(id -> new Object[] {
              objectiveId, objective.path("familyCode").asText(), id, section
            }).toList()
          );
        }
      }
    }
  }
}
