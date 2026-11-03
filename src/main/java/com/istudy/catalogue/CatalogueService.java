package com.istudy.catalogue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogueService {

  private final JdbcTemplate db;

  public CatalogueService(JdbcTemplate db) {
    this.db = db;
  }

  @Transactional
  public UUID ensureChapter(UUID offering, JsonNode chapter) {
    var existing = db.queryForList(
      "select chapter_id from chapters where offering_id=? and code=?",
      UUID.class,
      offering,
      chapter.path("code").asText()
    );
    if (!existing.isEmpty()) {
      db.update(
        "update chapters set title=?,sort_order=coalesce(?,sort_order),status='ACTIVE' where chapter_id=?",
        chapter.path("title").asText(),
        chapter.has("sortOrder") ? chapter.path("sortOrder").asInt() : null,
        existing.get(0)
      );
      return existing.get(0);
    }
    UUID id = UUID.randomUUID();
    db.update(
      "insert into chapters(chapter_id,offering_id,code,title,sort_order) values(?,?,?,?,?)",
      id,
      offering,
      chapter.path("code").asText(),
      chapter.path("title").asText(),
      chapter.path("sortOrder").asInt()
    );
    return id;
  }

  @Transactional
  public UUID ensureSection(UUID chapter, JsonNode topic) {
    var existing = db.queryForList(
      "select section_id from sections where chapter_id=? and code=?",
      UUID.class,
      chapter,
      topic.path("code").asText()
    );
    if (!existing.isEmpty()) {
      db.update(
        "update sections set title=?,sort_order=coalesce(?,sort_order),status='ACTIVE' where section_id=?",
        topic.path("title").asText(),
        topic.has("sortOrder") ? topic.path("sortOrder").asInt() : null,
        existing.get(0)
      );
      return existing.get(0);
    }
    UUID id = UUID.randomUUID();
    db.update(
      "insert into sections(section_id,chapter_id,code,title,sort_order) values(?,?,?,?,?)",
      id,
      chapter,
      topic.path("code").asText(),
      topic.path("title").asText(),
      topic.path("sortOrder").asInt()
    );
    return id;
  }
}
