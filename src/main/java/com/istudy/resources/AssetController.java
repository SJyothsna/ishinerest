package com.istudy.resources;

import com.istudy.identity.CurrentStudent;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

  private final JdbcTemplate db;
  private final CurrentStudent current;
  private final ObjectStorage storage;

  public AssetController(
    JdbcTemplate db,
    CurrentStudent current,
    ObjectStorage storage
  ) {
    this.db = db;
    this.current = current;
    this.storage = storage;
  }

  @PostMapping
  public Map<String, Object> upload(@RequestParam MultipartFile file)
    throws java.io.IOException {
    UUID owner = current.id();
    if (
      file.isEmpty() || file.getSize() > 10_000_000
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Choose an image smaller than 10 MB"
    );
    byte[] bytes = file.getBytes();
    var image = javax.imageio.ImageIO.read(
      new java.io.ByteArrayInputStream(bytes)
    );
    if (
      image == null || (long) image.getWidth() * image.getHeight() > 40_000_000
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Choose a valid PNG or JPEG image"
    );
    String type = file.getContentType();
    if (
      !Set.of("image/png", "image/jpeg").contains(type)
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Choose a PNG or JPEG image"
    );
    UUID id = UUID.randomUUID();
    String key = "notes/" + owner + "/" + id;
    storage.write(key, new java.io.ByteArrayInputStream(bytes));
    db.update(
      "insert into media_assets(asset_id,storage_provider,bucket_name,object_key,content_type,byte_size,scan_status,owner_user_id) values(?,'LOCAL','istudy-local',?,?,?,'CLEAN',?)",
      id,
      key,
      type,
      bytes.length,
      owner
    );
    return Map.of("assetId", id);
  }

  @GetMapping("/{id}/content")
  public ResponseEntity<byte[]> read(@PathVariable UUID id)
    throws java.io.IOException {
    UUID user = current.id();
    if (
      Boolean.TRUE.equals(
        db.queryForObject(
          "select exists(select 1 from media_assets a join content_documents d on d.asset_id=a.asset_id join app_users u on u.user_id=a.owner_user_id where a.asset_id=? and a.owner_user_id=? and u.content_admin=true and (d.document_type='TEXTBOOK' or (d.document_type='OTHER' and a.content_type='application/json')))",
          Boolean.class,
          id,
          user
        )
      )
    ) return content(id);
    boolean allowed = Boolean.TRUE.equals(
      db.queryForObject(
        "select exists(select 1 from media_assets a where a.asset_id=? and a.scan_status='CLEAN' and (a.owner_user_id=? or exists(select 1 from questions q where q.question_image_asset_id=a.asset_id and q.approval_status='PUBLISHED' and q.visibility='GLOBAL') or exists(select 1 from note_attachments na join study_notes n on n.note_id=na.note_id where na.asset_id=a.asset_id and (n.owner_user_id=? or n.note_type='GENERAL')) or exists(select 1 from content_documents d join past_papers p on p.document_id=d.document_id where d.asset_id=a.asset_id and d.licence_status='PERMITTED' and p.approval_status='PUBLISHED') or exists(select 1 from content_documents d join past_paper_marking_schemes m on m.document_id=d.document_id join past_papers p on p.paper_id=m.paper_id where d.asset_id=a.asset_id and d.licence_status='PERMITTED' and p.approval_status='PUBLISHED') or exists(select 1 from past_paper_question_assets qa join past_paper_questions pq on pq.paper_question_id=qa.paper_question_id join past_papers p on p.paper_id=pq.paper_id where qa.asset_id=a.asset_id and pq.review_status='APPROVED' and p.approval_status='PUBLISHED')))",
        Boolean.class,
        id,
        user,
        user
      )
    );
    if (!allowed) throw new ResponseStatusException(
      HttpStatus.NOT_FOUND,
      "Image not found"
    );
    return content(id);
  }

  public ResponseEntity<byte[]> content(UUID id) throws java.io.IOException {
    var row = db.queryForMap(
      "select object_key,content_type from media_assets where asset_id=?",
      id
    );
    try (var body = storage.read(row.get("object_key").toString())) {
      return ResponseEntity.ok()
        .contentType(
          MediaType.parseMediaType(row.get("content_type").toString())
        )
        .header(HttpHeaders.CACHE_CONTROL, "private, max-age=120")
        .header("X-Content-Type-Options", "nosniff")
        .body(body.readAllBytes());
    }
  }
}
