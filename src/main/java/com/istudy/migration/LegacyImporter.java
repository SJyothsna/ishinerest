package com.istudy.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("import-legacy")
@org.springframework.core.annotation.Order(1)
public class LegacyImporter implements ApplicationRunner {

  private final JdbcTemplate target;
  private final ObjectMapper json;
  private final String sourceUrl;

  public LegacyImporter(
    JdbcTemplate db,
    ObjectMapper json,
    @Value(
      "${app.import.source-url:jdbc:h2:file:./data/testdb;ACCESS_MODE_DATA=r;IFEXISTS=TRUE}"
    ) String sourceUrl
  ) {
    this.target = db;
    this.json = json;
    this.sourceUrl = sourceUrl;
  }

  static UUID id(String kind, Object value) {
    return UUID.nameUUIDFromBytes(
      ("istudy-import:" + kind + ":" + value).getBytes(StandardCharsets.UTF_8)
    );
  }

  static String text(Map<String, Object> row, String key) {
    return row.get(key) == null ? "" : row.get(key).toString();
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (
      target.queryForObject("select count(*) from curricula", Integer.class) > 0
    ) {
      importMissingAdmins();
      return;
    }
    var source = new JdbcTemplate(
      new DriverManagerDataSource(sourceUrl, "sa", "")
    );
    UUID curriculum = id("curriculum", "legacy");
    target.update(
      "insert into curricula(curriculum_id,code,name) values(?,'IMPORTED','Existing iStudy catalogue')",
      curriculum
    );
    for (var user : source.queryForList("select * from users")) {
      String role = text(user, "ROLE");
      boolean admin = role.equals("ADMIN");
      if (!Set.of("STUDENT", "PARENT", "ADMIN").contains(role)) continue;
      UUID uid = id("user", user.get("USER_ID"));
      target.update(
        "insert into app_users(user_id,email,display_name,email_verified,account_role,status) values(?,?,?,?,?,?)",
        uid,
        text(user, "EMAIL").toLowerCase(Locale.ROOT),
        user.get("NAME"),
        user.get("EMAIL_VERIFIED"),
        admin ? "STUDENT" : role,
        Boolean.FALSE.equals(user.get("IS_ACTIVE")) ? "SUSPENDED" : "ACTIVE"
      );
      target.update(
        "insert into local_credentials values(?,?)",
        uid,
        user.get("PASSWORD_HASH")
      );
      target.update(
        "insert into auth_identities(user_id,provider,provider_subject) values(?,'LOCAL',?)",
        uid,
        uid.toString()
      );
      if (admin) target.update(
        "update app_users set content_admin=true where user_id=?",
        uid
      );
      if (role.equals("STUDENT") || admin) target.update(
        "insert into student_profiles(student_id) values(?)",
        uid
      );
    }
    var oldSubjects = source.queryForList("select * from subjects");
    var oldChapters = source.queryForList("select * from chapters");
    var oldQuestions = source.queryForList("select * from questions");
    var offerings = new LinkedHashMap<String, UUID>();
    for (var level : source.queryForList(
      "select * from classes"
    )) target.update(
      "insert into academic_levels(level_id,code,name) values(?,?,?)",
      id("level", level.get("CLASS_ID")),
      "import-" + level.get("CLASS_ID"),
      level.get("CLASS_NAME")
    );
    for (var subject : oldSubjects) {
      String code = text(subject, "SUBJECT_ID");
      UUID sid = id("subject", code);
      target.update(
        "insert into subjects(subject_id,code,name) values(?,?,?)",
        sid,
        code,
        subject.get("SUBJECT_NAME")
      );
      for (var link : source.queryForList(
        "select * from class_subjects where subject_id=?",
        code
      )) {
        String classId = text(link, "CLASS_ID");
        UUID offering = id("offering", classId + ":" + code);
        offerings.put(classId + ":" + code, offering);
        target.update(
          "insert into subject_offerings(offering_id,curriculum_id,level_id,subject_id,syllabus_version,status) values(?,?,?,?,?,'PUBLISHED')",
          offering,
          curriculum,
          id("level", classId),
          sid,
          "imported-v1"
        );
        for (var chapter : oldChapters) if (
          code.equals(text(chapter, "SUBJECT_ID"))
        ) {
          String ch = text(chapter, "CHAPTER_ID");
          UUID chid = id("chapter", offering + ":" + ch);
          target.update(
            "insert into chapters(chapter_id,offering_id,code,title,sort_order) values(?,?,?,?,?)",
            chid,
            offering,
            ch,
            chapter.get("CHAPTER_NAME"),
            oldChapters.indexOf(chapter)
          );
          var sectionCodes = new LinkedHashSet<String>();
          for (var q : oldQuestions) if (
            ch.equals(text(q, "CHAPTER_ID"))
          ) sectionCodes.add(
            text(q, "SECTION_ID").isBlank() ? "general" : text(q, "SECTION_ID")
          );
          if (sectionCodes.isEmpty()) sectionCodes.add("general");
          int order = 0;
          for (String section : sectionCodes) target.update(
            "insert into sections(section_id,chapter_id,code,title,sort_order) values(?,?,?,?,?)",
            id("section", chid + ":" + section),
            chid,
            section,
            section.equals("general") ? "Entire chapter" : section,
            order++
          );
        }
      }
    }
    for (var q : oldQuestions) {
      UUID qid = id("question", q.get("QUESTION_ID"));
      UUID asset = importImage(text(q, "QUESTION_IMAGE_URL"));
      int type = ((Number) q.get("QUESTION_TYPE")).intValue();
      String typeName = type == 4
        ? "MULTIPLE_CHOICE"
        : type == 2 ? "TEXT" : type == 3 ? "WORKED_EXAMPLE" : "SINGLE_CHOICE";
      String difficulty =
        switch (text(q, "DIFFICULTY_LEVEL").toUpperCase(Locale.ROOT)) {
          case "ADVANCED", "HARD" -> "ADVANCED";
          case "INTERMEDIATE", "MEDIUM" -> "INTERMEDIATE";
          default -> "FOUNDATION";
        };
      var snapshot = json
        .createObjectNode()
        .put("prompt", text(q, "QUESTION_TEXT"))
        .put(
          "type",
          type == 4
            ? "multiple"
            : type == 2 ? "text" : type == 3 ? "worked" : "single"
        )
        .put("correctAnswer", text(q, "CORRECT_ANSWER"))
        .put("explanation", text(q, "EXPLANATION"))
        .put("difficulty", difficulty);
      if (asset != null) snapshot.put("imageAssetId", asset.toString());
      var options = snapshot.putArray("options");
      for (String key : List.of("A", "B", "C", "D", "E", "F")) if (
        !text(q, "OPTION_" + key).isBlank()
      ) options
        .addObject()
        .put("id", key)
        .put("text", text(q, "OPTION_" + key));
      if (type == 4) {
        String answers = text(q, "CORRECT_ANSWERS");
        var array = snapshot.putArray("correctAnswers");
        try {
          var parsed = json.readTree(answers);
          if (parsed.isArray()) parsed.forEach(array::add);
          else for (String a : answers.split(",")) array.add(a.trim());
        } catch (Exception e) {
          for (String a : answers.split(",")) array.add(a.trim());
        }
      }
      target.update(
        "insert into questions(question_id,question_text,correct_answer,question_type,explanation,difficulty_level,approval_status,visibility) values(?,?,?,?,?,?,?,?)",
        qid,
        text(q, "QUESTION_TEXT"),
        q.get("CORRECT_ANSWER"),
        typeName,
        q.get("EXPLANATION"),
        difficulty,
        (text(q, "QUESTION_TEXT").isBlank() && asset == null) ||
          (type != 3 &&
            text(q, type == 4 ? "CORRECT_ANSWERS" : "CORRECT_ANSWER").isBlank())
          ? "DRAFT"
          : "PUBLISHED",
        Set.of("PUBLIC", "SHARED").contains(text(q, "VISIBILITY"))
          ? "GLOBAL"
          : "PRIVATE"
      );
      if (asset != null) target.update(
        "update questions set question_image_asset_id=? where question_id=?",
        asset,
        qid
      );
      target.update(
        "insert into question_revisions(question_id,revision_number,content_snapshot) values(?,1,?)",
        qid,
        snapshot.toString()
      );
      String chapter = text(q, "CHAPTER_ID");
      for (var placement : target.queryForList(
        "select chapter_id from chapters where code=?",
        chapter
      )) {
        UUID chid = (UUID) placement.get("chapter_id");
        String section = text(q, "SECTION_ID").isBlank()
          ? "general"
          : text(q, "SECTION_ID");
        target.update(
          "insert into question_placements(question_id,section_id,usage_type,question_set) values(?,?,?,?)",
          qid,
          id("section", chid + ":" + section),
          text(q, "USAGE_TYPE").toUpperCase(Locale.ROOT),
          text(q, "QUESTION_SET").isBlank()
            ? 1
            : Integer.parseInt(text(q, "QUESTION_SET"))
        );
      }
    }
    for (var link : source.queryForList(
      "select ss.*,st.class_id from student_subject ss join students st on st.student_id=ss.student_id"
    )) {
      UUID offering = offerings.get(
        text(link, "CLASS_ID") + ":" + text(link, "SUBJECT_ID")
      );
      if (offering != null) target.update(
        "insert into student_subjects(student_id,offering_id) values(?,?)",
        id("user", link.get("STUDENT_ID")),
        offering
      );
    }
    for (var flag : source.queryForList("select * from flagged_questions")) {
      UUID owner = id("user", flag.get("STUDENT_ID"));
      if (
        Boolean.TRUE.equals(
          target.queryForObject(
            "select exists(select 1 from student_profiles where student_id=?)",
            Boolean.class,
            owner
          )
        )
      ) target.update(
        "insert into student_question_flags(student_id,question_id) values(?,?)",
        owner,
        id("question", flag.get("QUESTION_ID"))
      );
    }
    for (var link : source.queryForList("select * from parent_student")) {
      String status = text(link, "STATUS");
      target.update(
        "insert into parent_student_links(parent_user_id,student_id,status,verified_at) values(?,?,?,?)",
        id("user", link.get("PARENT_USER_ID")),
        id("user", link.get("STUDENT_USER_ID")),
        status.equals("APPROVED")
          ? "VERIFIED"
          : status.equals("PENDING") ? "PENDING" : "REVOKED",
        link.get("APPROVED_AT")
      );
    }
    // Preserve source rows whose historical revision cannot be established; do not fabricate snapshots.
    for (String table : List.of(
      "PRACTICE_SESSION_DETAILS",
      "TEST_DETAILS"
    )) for (var evidence : source.queryForList(
      "select * from " + table
    )) target.update(
      "insert into imported_evidence(source_table,payload) values(?,?)",
      table,
      encode(evidence)
    );
    for (var note : source.queryForList("select * from student_notes")) {
      UUID owner = id("user", note.get("STUDENT_ID")), asset = importImage(
        text(note, "IMAGE_URL")
      );
      if (asset != null) target.update(
        "update media_assets set owner_user_id=? where asset_id=?",
        owner,
        asset
      );
      for (var ch : target.queryForList(
        "select c.chapter_id,c.offering_id from chapters c join student_subjects ss on ss.offering_id=c.offering_id where ss.student_id=? and c.code=?",
        owner,
        note.get("CHAPTER_ID")
      )) {
        UUID noteId = id("note", note.get("ID") + ":" + ch.get("offering_id"));
        target.update(
          "insert into study_notes(note_id,offering_id,chapter_id,owner_user_id,note_type,title,content_text,color) values(?,?,?,?,'STUDENT',?,?,?)",
          noteId,
          ch.get("offering_id"),
          ch.get("chapter_id"),
          owner,
          note.get("TITLE"),
          note.get("CONTENT"),
          text(note, "COLOR").isBlank() ? "#FFF9C4" : note.get("COLOR")
        );
        if (asset != null) target.update(
          "insert into note_attachments(note_id,asset_id) values(?,?)",
          noteId,
          asset
        );
      }
    }
    for (var note : source.queryForList(
      "select * from common_notes where is_active=true"
    )) {
      UUID asset = importImage(text(note, "IMAGE_URL"));
      for (var ch : target.queryForList(
        "select chapter_id,offering_id from chapters where code=?",
        note.get("CHAPTER_ID")
      )) {
        UUID noteId = id(
          "common-note",
          note.get("ID") + ":" + ch.get("offering_id")
        );
        target.update(
          "insert into study_notes(note_id,offering_id,chapter_id,note_type,title,content_text,color) values(?,?,?,'GENERAL',?,?,?)",
          noteId,
          ch.get("offering_id"),
          ch.get("chapter_id"),
          note.get("TITLE"),
          note.get("CONTENT"),
          text(note, "COLOR").isBlank() ? "#BBDEFB" : note.get("COLOR")
        );
        if (asset != null) target.update(
          "insert into note_attachments(note_id,asset_id) values(?,?)",
          noteId,
          asset
        );
      }
    }
  }

  private void importMissingAdmins() {
    var source = new JdbcTemplate(
      new DriverManagerDataSource(sourceUrl, "sa", "")
    );
    for (var user : source.queryForList(
      "select * from users where role='ADMIN'"
    )) {
      UUID uid = id("user", user.get("USER_ID"));
      if (
        target.queryForObject(
          "select count(*) from app_users where user_id=?",
          Integer.class,
          uid
        ) >
        0
      ) {
        target.update(
          "update app_users set content_admin=true where user_id=?",
          uid
        );
        continue;
      }
      target.update(
        "insert into app_users(user_id,email,display_name,email_verified,account_role,status,content_admin) values(?,?,?,?,'STUDENT',?,true)",
        uid,
        text(user, "EMAIL").toLowerCase(Locale.ROOT),
        user.get("NAME"),
        user.get("EMAIL_VERIFIED"),
        Boolean.FALSE.equals(user.get("IS_ACTIVE")) ? "SUSPENDED" : "ACTIVE"
      );
      target.update(
        "insert into local_credentials values(?,?)",
        uid,
        user.get("PASSWORD_HASH")
      );
      target.update(
        "insert into auth_identities(user_id,provider,provider_subject) values(?,'LOCAL',?)",
        uid,
        uid.toString()
      );
      target.update("insert into student_profiles(student_id) values(?)", uid);
    }
  }

  private UUID importImage(String url) {
    if (url.isBlank()) return null;
    try {
      String path = java.net.URI.create(url).getPath();
      if (path.startsWith("uploads/")) path = "/" + path;
      if (!path.startsWith("/uploads/")) return null;
      var root = java.nio.file.Path.of("public/uploads")
        .toAbsolutePath()
        .normalize();
      var source = root.resolve(path.substring(9)).normalize();
      if (
        !source.startsWith(root) || !java.nio.file.Files.isRegularFile(source)
      ) return null;
      var image = javax.imageio.ImageIO.read(source.toFile());
      if (image == null) return null;
      UUID asset = id("asset", path);
      if (
        target.queryForObject(
          "select count(*) from media_assets where asset_id=?",
          Integer.class,
          asset
        ) >
        0
      ) return asset;
      String key = "legacy/" + path.substring(9);
      var dest = java.nio.file.Path.of("data/private-assets").resolve(key);
      java.nio.file.Files.createDirectories(dest.getParent());
      java.nio.file.Files.copy(
        source,
        dest,
        java.nio.file.StandardCopyOption.REPLACE_EXISTING
      );
      target.update(
        "insert into media_assets(asset_id,storage_provider,bucket_name,object_key,content_type,byte_size,scan_status) values(?,'LOCAL','istudy-local',?,?,?,'CLEAN')",
        asset,
        key,
        path.endsWith(".png") ? "image/png" : "image/jpeg",
        java.nio.file.Files.size(source)
      );
      return asset;
    } catch (Exception e) {
      throw new IllegalStateException("Could not import a question image", e);
    }
  }

  private String encode(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
