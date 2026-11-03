package com.istudy.migration;

import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.util.*;
import org.springframework.boot.*;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("import-legacy")
@Order(2)
public class PaperImporter implements ApplicationRunner {

  private final JdbcTemplate db;
  private final ObjectMapper json;
  private final String sourceRoot;

  public PaperImporter(
    JdbcTemplate db,
    ObjectMapper json,
    @org.springframework.beans.factory.annotation.Value(
      "${app.import.paper-root:data/legacy-paper-sources}"
    ) String sourceRoot
  ) {
    this.db = db;
    this.json = json;
    this.sourceRoot = sourceRoot;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) throws Exception {
    Path root = Path.of(sourceRoot).toAbsolutePath().normalize();
    if (!Files.isDirectory(root)) return;
    UUID exam = LegacyImporter.id("exam", "legacy-lc");
    if (
      db.queryForObject(
        "select count(*) from exam_programmes where exam_id=?",
        Integer.class,
        exam
      ) ==
      0
    ) db.update(
      "insert into exam_programmes(exam_id,code,name) values(?,'IMPORTED_LC','Previous examination papers')",
      exam
    );
    try (var paths = Files.walk(root)) {
      for (Path path : paths
        .filter(p -> p.getFileName().toString().equals("papers.json"))
        .toList()) for (JsonNode paper : json.readTree(path.toFile())) {
        String code = paper.path("subjectId").asText(), filename = paper
          .path("examUrl")
          .asText();
        if (filename.isBlank()) continue;
        UUID paperId = LegacyImporter.id("paper", code + ":" + filename);
        if (
          db.queryForObject(
            "select count(*) from past_papers where paper_id=?",
            Integer.class,
            paperId
          ) >
          0
        ) continue;
        Path file = path.getParent().resolve(filename).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) continue;
        UUID document = document(
          file,
          root,
          paper.path("title").asText(),
          "EXAM_PAPER"
        );
        db.update(
          "insert into past_papers(paper_id,exam_id,document_id,exam_year,exam_session,paper_code,approval_status) values(?,?,?,?,?,?,'PUBLISHED')",
          paperId,
          exam,
          document,
          paper.path("year").asInt(),
          paper.path("paperType").asText(),
          code + ":" + filename
        );
        for (UUID offering : db.queryForList(
          "select o.offering_id from subject_offerings o join subjects s on s.subject_id=o.subject_id where s.code=?",
          UUID.class,
          code
        )) db.update(
          "insert into past_paper_offerings values(?,?)",
          paperId,
          offering
        );
        String marking = paper.path("markingUrl").asText();
        Path mark = path.getParent().resolve(marking).normalize();
        if (
          !marking.isBlank() &&
          mark.startsWith(root) &&
          Files.isRegularFile(mark)
        ) {
          UUID doc = document(mark, root, "Marking scheme", "MARK_SCHEME");
          db.update(
            "insert into past_paper_marking_schemes(paper_id,document_id) values(?,?)",
            paperId,
            doc
          );
        }
      }
    }
    // Preserve each chapter occurrence and its original question/marking images.
    try (var paths = Files.walk(root)) {
      for (Path path : paths
        .filter(p -> p.getFileName().toString().equals("questions.json"))
        .toList()) for (JsonNode q : json.readTree(path.toFile())) {
        String oldChapter = q.path("chapterId").asText();
        UUID occurrence = LegacyImporter.id(
          "paper-occurrence",
          path + ":" + q.toString()
        );
        if (
          db.queryForObject(
            "select count(*) from past_paper_questions where paper_question_id=?",
            Integer.class,
            occurrence
          ) >
          0
        ) continue;
        var questionAssets = json.createArrayNode();
        for (var image : q.path("question")) {
          Path file = path
            .getParent()
            .resolve(image.asText() + ".png")
            .normalize();
          if (
            file.startsWith(root) && Files.isRegularFile(file)
          ) questionAssets.add(asset(file, root, "image/png").toString());
        }
        var markingAssets = json.createArrayNode();
        for (var image : q.path("markingUrl")) {
          Path file = path
            .getParent()
            .resolve(image.asText() + ".png")
            .normalize();
          if (
            file.startsWith(root) && Files.isRegularFile(file)
          ) markingAssets.add(asset(file, root, "image/png").toString());
        }
        UUID paperId = LegacyImporter.id(
          "extracted-paper",
          oldChapter.substring(0, Math.min(5, oldChapter.length())) +
          ":" +
          q.path("year").asText() +
          ":" +
          q.path("paperName").asText() +
          ":" +
          q.path("paperType").asText()
        );
        if (
          db.queryForObject(
            "select count(*) from past_papers where paper_id=?",
            Integer.class,
            paperId
          ) ==
          0
        ) db.update(
          "insert into past_papers(paper_id,exam_id,exam_year,exam_session,paper_code,approval_status) values(?,?,?,?,?,'PUBLISHED')",
          paperId,
          exam,
          q.path("year").asInt(),
          q.path("paperType").asText(),
          "extracted:" + paperId
        );
        db.update(
          "insert into past_paper_questions(paper_question_id,paper_id,question_number,part_label,extracted_text,review_status) values(?,?,?,?,?,'APPROVED')",
          occurrence,
          paperId,
          q.path("questionnumber").asText(),
          occurrence.toString(),
          q.path("paperName").asText()
        );
        int order = 0;
        for (JsonNode asset : questionAssets) db.update(
          "insert into past_paper_question_assets values(?,?,'QUESTION',?)",
          occurrence,
          UUID.fromString(asset.asText()),
          order++
        );
        order = 0;
        for (JsonNode asset : markingAssets) db.update(
          "insert into past_paper_question_assets values(?,?,'MARKING_SCHEME',?)",
          occurrence,
          UUID.fromString(asset.asText()),
          order++
        );
        for (var ch : db.queryForList(
          "select chapter_id from chapters where code=?",
          oldChapter
        )) db.update(
          "insert into past_paper_question_chapters values(?,?)",
          occurrence,
          ch.get("chapter_id")
        );
      }
    }
  }

  private UUID document(Path file, Path root, String title, String type)
    throws Exception {
    UUID id = LegacyImporter.id("document", root.relativize(file));
    if (
      db.queryForObject(
        "select count(*) from content_documents where document_id=?",
        Integer.class,
        id
      ) ==
      0
    ) db.update(
      "insert into content_documents(document_id,asset_id,document_type,title,licence_status,processing_status) values(?,?,?,?,'PERMITTED','READY')",
      id,
      asset(file, root, "application/pdf"),
      type,
      title
    );
    return id;
  }

  private UUID asset(Path file, Path root, String type) throws Exception {
    String key = "papers/" + root.relativize(file).toString();
    UUID id = LegacyImporter.id("asset", key);
    if (
      db.queryForObject(
        "select count(*) from media_assets where asset_id=?",
        Integer.class,
        id
      ) >
      0
    ) return id;
    Path dest = Path.of("data/private-assets").resolve(key);
    Files.createDirectories(dest.getParent());
    Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
    db.update(
      "insert into media_assets(asset_id,storage_provider,bucket_name,object_key,content_type,byte_size,scan_status) values(?,'LOCAL','istudy-local',?,?,?,'CLEAN')",
      id,
      key,
      type,
      Files.size(file)
    );
    return id;
  }
}
