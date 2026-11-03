package com.istudy.resources;

import com.istudy.identity.CurrentStudent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/notes")
public class NotesController {

  private final JdbcTemplate db;
  private final CurrentStudent current;

  public NotesController(JdbcTemplate db, CurrentStudent current) {
    this.db = db;
    this.current = current;
  }

  public record Note(
    @NotNull UUID offeringId,
    UUID chapterId,
    UUID sectionId,
    @NotBlank @Size(max = 300) String title,
    @Size(max = 100000) String content,
    @Pattern(regexp = "#[0-9A-Fa-f]{6}") String color,
    List<UUID> attachmentIds
  ) {}

  @GetMapping
  public List<Map<String, Object>> notes(
    @RequestParam UUID offeringId,
    @RequestParam(required = false) UUID chapterId
  ) {
    return db.queryForList(
      "select n.note_id as \"id\",n.chapter_id as \"chapterId\",c.title as \"chapterName\",n.note_type as \"kind\",n.title,n.content_text as \"content\",n.color,n.updated_at as \"updatedAt\" from study_notes n left join chapters c on c.chapter_id=n.chapter_id where n.offering_id=? and (?::uuid is null or n.chapter_id=?::uuid) and (n.owner_user_id=? or n.note_type='GENERAL') order by n.updated_at desc",
      offeringId,
      chapterId,
      chapterId,
      current.id()
    );
  }

  private void attachments(UUID note, List<UUID> assets) {
    var ids = assets == null ? List.<UUID>of() : assets;
    if (ids.size() > 10) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Use up to 10 images per note"
    );
    for (UUID asset : ids) if (
      !Boolean.TRUE.equals(
        db.queryForObject(
          "select exists(select 1 from media_assets where asset_id=? and owner_user_id=? and scan_status='CLEAN')",
          Boolean.class,
          asset,
          current.id()
        )
      )
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Attachment is not available"
    );
    db.update("delete from note_attachments where note_id=?", note);
    int pos = 0;
    for (UUID asset : new LinkedHashSet<>(ids)) db.update(
      "insert into note_attachments(note_id,asset_id,sort_order) values(?,?,?)",
      note,
      asset,
      pos++
    );
  }

  private void scope(Note n) {
    if (
      n.chapterId() != null &&
      !Boolean.TRUE.equals(
        db.queryForObject(
          "select exists(select 1 from chapters where chapter_id=? and offering_id=?)",
          Boolean.class,
          n.chapterId(),
          n.offeringId()
        )
      )
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Chapter does not belong to this subject"
    );
    if (
      n.sectionId() != null &&
      !Boolean.TRUE.equals(
        db.queryForObject(
          "select exists(select 1 from sections s join chapters c on c.chapter_id=s.chapter_id where s.section_id=? and c.offering_id=? and (?::uuid is null or c.chapter_id=?::uuid))",
          Boolean.class,
          n.sectionId(),
          n.offeringId(),
          n.chapterId(),
          n.chapterId()
        )
      )
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Topic does not belong to this subject"
    );
  }

  @PostMapping
  @Transactional
  public Map<String, Object> create(@Valid @RequestBody Note note) {
    scope(note);
    UUID id = UUID.randomUUID();
    db.update(
      "insert into study_notes(note_id,offering_id,chapter_id,section_id,owner_user_id,note_type,title,content_text,color) values(?,?,?,?,?,'STUDENT',?,?,?)",
      id,
      note.offeringId(),
      note.chapterId(),
      note.sectionId(),
      current.id(),
      note.title(),
      note.content(),
      note.color() == null ? "#FFF9C4" : note.color()
    );
    attachments(id, note.attachmentIds());
    return Map.of("id", id);
  }

  @PutMapping("/{id}")
  @Transactional
  public Map<String, Object> update(
    @PathVariable UUID id,
    @Valid @RequestBody Note note
  ) {
    scope(note);
    if (
      db.update(
        "update study_notes set title=?,content_text=?,color=?,updated_at=current_timestamp where note_id=? and owner_user_id=? and note_type='STUDENT' and offering_id=?",
        note.title(),
        note.content(),
        note.color() == null ? "#FFF9C4" : note.color(),
        id,
        current.id(),
        note.offeringId()
      ) ==
      0
    ) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found");
    attachments(id, note.attachmentIds());
    return Map.of("id", id);
  }

  @GetMapping("/{id}/attachments")
  public List<Map<String, Object>> attachments(@PathVariable UUID id) {
    return db.queryForList(
      "select a.asset_id as \"assetId\" from note_attachments a join study_notes n on n.note_id=a.note_id where n.note_id=? and (n.owner_user_id=? or n.note_type='GENERAL') order by a.sort_order",
      id,
      current.id()
    );
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable UUID id) {
    if (
      db.update(
        "delete from study_notes where note_id=? and owner_user_id=? and note_type='STUDENT'",
        id,
        current.id()
      ) ==
      0
    ) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found");
  }
}
