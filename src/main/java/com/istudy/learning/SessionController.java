package com.istudy.learning;

import static com.istudy.learning.LearningModels.*;

import com.istudy.identity.CurrentStudent;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/practice-sessions")
public class SessionController {

  private final SessionService sessions;
  private final CurrentStudent current;
  private final com.istudy.resources.AssetController assets;

  public SessionController(
    SessionService sessions,
    CurrentStudent current,
    com.istudy.resources.AssetController assets
  ) {
    this.sessions = sessions;
    this.current = current;
    this.assets = assets;
  }

  @PostMapping
  public Map<String, Object> create(@Valid @RequestBody CreateSession request) {
    return sessions.create(current.id(), null, request);
  }

  @GetMapping("/{id}")
  public Map<String, Object> get(@PathVariable UUID id) {
    return sessions.read(id, current.id(), null);
  }

  @PutMapping("/{id}/drafts")
  public void drafts(
    @PathVariable UUID id,
    @Valid @RequestBody Drafts request
  ) {
    sessions.drafts(id, current.id(), null, request.answers());
  }

  @PostMapping("/{id}/check")
  public Map<String, Object> check(
    @PathVariable UUID id,
    @Valid @RequestBody Answer answer
  ) {
    return sessions.check(id, current.id(), answer);
  }

  @PutMapping("/{id}/questions/{question}/flag")
  public void flag(
    @PathVariable UUID id,
    @PathVariable UUID question,
    @Valid @RequestBody ReviewFlag request
  ) {
    sessions.reviewFlag(id, current.id(), null, question, request.flagged());
  }

  @PostMapping("/{id}/questions/{question}/issues")
  public void issue(
    @PathVariable UUID id,
    @PathVariable UUID question,
    @Valid @RequestBody QuestionIssue request
  ) {
    sessions.reportIssue(id, current.id(), null, question, request.reason());
  }

  @PostMapping("/{id}/submit")
  public Map<String, Object> submit(
    @PathVariable UUID id,
    @Valid @RequestBody Submit request
  ) {
    return sessions.submit(id, current.id(), null, request);
  }

  @GetMapping("/{id}/assets/{assetId}")
  public org.springframework.http.ResponseEntity<byte[]> asset(
    @PathVariable UUID id,
    @PathVariable UUID assetId
  ) throws java.io.IOException {
    var session = sessions.read(id, current.id(), null);
    for (Object item : (java.util.List<?>) session.get("questions")) if (
      ((com.fasterxml.jackson.databind.JsonNode) item).path("imageAssetId")
        .asText()
        .equals(assetId.toString())
    ) return assets.content(assetId);
    throw new org.springframework.web.server.ResponseStatusException(
      org.springframework.http.HttpStatus.NOT_FOUND,
      "Image not found"
    );
  }
}
