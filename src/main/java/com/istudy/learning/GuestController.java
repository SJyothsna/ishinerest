package com.istudy.learning;

import static com.istudy.learning.LearningModels.*;

import com.istudy.identity.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/guest-practice/sessions")
public class GuestController {

  private final JdbcTemplate db;
  private final SessionService sessions;
  private final CurrentStudent current;
  private final com.istudy.resources.AssetController assets;
  private final boolean secureCookies;

  public GuestController(
    JdbcTemplate db,
    SessionService sessions,
    CurrentStudent current,
    com.istudy.resources.AssetController assets,
    @org.springframework.beans.factory.annotation.Value(
      "${app.cookies.secure:false}"
    ) boolean secureCookies
  ) {
    this.db = db;
    this.sessions = sessions;
    this.current = current;
    this.assets = assets;
    this.secureCookies = secureCookies;
  }

  public record Sample(UUID offeringId, UUID chapterId) {}

  private UUID guest(String secret) {
    if (secret == null) throw new ResponseStatusException(
      HttpStatus.UNAUTHORIZED,
      "Sample session has expired"
    );
    return db
      .queryForList(
        "select guest_id from guest_principals where secret_hash=? and expires_at>current_timestamp",
        UUID.class,
        LocalAuthService.hash(secret)
      )
      .stream()
      .findFirst()
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.UNAUTHORIZED,
          "Sample session has expired"
        )
      );
  }

  @PostMapping
  public Map<String, Object> create(
    @RequestBody Sample sample,
    @CookieValue(value = "istudy_guest", required = false) String cookie,
    HttpServletResponse response
  ) {
    UUID guest;
    try {
      guest = guest(cookie);
    } catch (ResponseStatusException e) {
      String secret = LocalAuthService.secret();
      guest = UUID.randomUUID();
      db.update(
        "insert into guest_principals(guest_id,secret_hash,expires_at) values(?,?,?)",
        guest,
        LocalAuthService.hash(secret),
        java.sql.Timestamp.from(java.time.Instant.now().plusSeconds(86400))
      );
      response.addHeader(
        HttpHeaders.SET_COOKIE,
        ResponseCookie.from("istudy_guest", secret)
          .httpOnly(true)
          .secure(secureCookies)
          .sameSite("Strict")
          .path("/api/v1/guest-practice")
          .maxAge(86400)
          .build()
          .toString()
      );
    }
    return sessions.create(
      null,
      guest,
      new CreateSession(
        sample.offeringId(),
        sample.chapterId(),
        List.of(),
        "SAMPLE",
        "QUICK",
        sample.chapterId() == null ? "SUBJECT_ALL" : "CHAPTER_ALL",
        "ALL",
        7,
        null
      )
    );
  }

  private UUID signedIn() {
    try {
      return current.id();
    } catch (ResponseStatusException e) {
      return null;
    }
  }

  @GetMapping("/{id}")
  public Map<String, Object> get(
    @PathVariable UUID id,
    @CookieValue("istudy_guest") String cookie
  ) {
    return sessions.read(id, signedIn(), guest(cookie));
  }

  @PutMapping("/{id}/drafts")
  public void drafts(
    @PathVariable UUID id,
    @CookieValue("istudy_guest") String cookie,
    @Valid @RequestBody Drafts request
  ) {
    sessions.drafts(id, null, guest(cookie), request.answers());
  }

  @PutMapping("/{id}/questions/{question}/flag")
  public void flag(
    @PathVariable UUID id,
    @PathVariable UUID question,
    @CookieValue("istudy_guest") String cookie,
    @Valid @RequestBody ReviewFlag request
  ) {
    sessions.reviewFlag(id, null, guest(cookie), question, request.flagged());
  }

  @PostMapping("/{id}/questions/{question}/issues")
  public void issue(
    @PathVariable UUID id,
    @PathVariable UUID question,
    @CookieValue("istudy_guest") String cookie,
    @Valid @RequestBody QuestionIssue request
  ) {
    sessions.reportIssue(id, null, guest(cookie), question, request.reason());
  }

  @PostMapping("/{id}/submit")
  public Map<String, Object> submit(
    @PathVariable UUID id,
    @CookieValue("istudy_guest") String cookie,
    @Valid @RequestBody Submit request
  ) {
    return sessions.submit(id, null, guest(cookie), request);
  }

  @GetMapping("/{id}/assets/{assetId}")
  public org.springframework.http.ResponseEntity<byte[]> asset(
    @PathVariable UUID id,
    @PathVariable UUID assetId,
    @CookieValue("istudy_guest") String cookie
  ) throws java.io.IOException {
    var session = sessions.read(id, null, guest(cookie));
    boolean allowed = false;
    for (Object item : (java.util.List<?>) session.get("questions")) {
      var question = (com.fasterxml.jackson.databind.JsonNode) item;
      if (
        question.path("imageAssetId").asText().equals(assetId.toString())
      ) allowed = true;
    }
    if (!allowed) throw new ResponseStatusException(
      HttpStatus.NOT_FOUND,
      "Image not found"
    );
    return assets.content(assetId);
  }

  @PostMapping("/{id}/claim")
  public Map<String, Object> claim(
    @PathVariable UUID id,
    @CookieValue("istudy_guest") String cookie
  ) {
    return sessions.claim(id, guest(cookie), current.id());
  }
}
