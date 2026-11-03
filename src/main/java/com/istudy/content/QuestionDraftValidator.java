package com.istudy.content;

import com.fasterxml.jackson.databind.*;
import com.istudy.identity.LocalAuthService;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class QuestionDraftValidator {

  public void validate(JsonNode q) {
    String type = q.path("type").asText();
    if (
      !Set.of("single", "multiple", "text", "worked").contains(type) ||
      !q.path("prompt").isTextual() ||
      q.path("prompt").asText().isBlank() ||
      !q.path("explanation").isTextual() ||
      q.path("explanation").asText().isBlank() ||
      !Set.of("FOUNDATION", "INTERMEDIATE", "ADVANCED").contains(
        q.path("difficulty").asText()
      )
    ) throw new IllegalArgumentException(
      "Provide a supported type, prompt, explanation and difficulty"
    );
    if (
      q.path("prompt").asText().length() > 10000
    ) throw new IllegalArgumentException("Question is too long");
    if (q.has("acceptedAnswers")) {
      if (
        !q.get("acceptedAnswers").isArray() ||
        q.get("acceptedAnswers").size() > 20
      ) throw new IllegalArgumentException("Use up to 20 accepted answers");
      for (var a : q.get("acceptedAnswers")) if (
        !a.isTextual() || a.asText().isBlank()
      ) throw new IllegalArgumentException(
        "Accepted answers must be non-empty strings"
      );
    }
    if (type.equals("worked")) return;
    if (type.equals("text")) {
      if (
        !q.path("correctAnswer").isTextual() ||
        q.path("correctAnswer").asText().isBlank()
      ) throw new IllegalArgumentException("Provide the expected text answer");
      return;
    }
    if (
      !q.path("options").isArray() ||
      q.path("options").size() < 2 ||
      q.path("options").size() > 6
    ) throw new IllegalArgumentException("Use two to six options");
    var ids = new HashSet<String>();
    for (var option : q.path("options")) if (
      !option.path("id").isTextual() ||
      option.path("id").asText().isBlank() ||
      !option.path("text").isTextual() ||
      option.path("text").asText().isBlank() ||
      !ids.add(option.path("id").asText())
    ) throw new IllegalArgumentException(
      "Options need unique string IDs and non-empty text"
    );
    if (
      type.equals("single") &&
      (!q.path("correctAnswer").isTextual() ||
        !ids.contains(q.path("correctAnswer").asText()))
    ) throw new IllegalArgumentException(
      "The correct answer must match an option ID"
    );
    if (type.equals("multiple")) {
      var selected = new HashSet<String>();
      if (
        !q.path("correctAnswers").isArray() ||
        q.path("correctAnswers").isEmpty()
      ) throw new IllegalArgumentException(
        "Provide at least one correct option"
      );
      for (var answer : q.path("correctAnswers")) if (
        !answer.isTextual() ||
        !ids.contains(answer.asText()) ||
        !selected.add(answer.asText())
      ) throw new IllegalArgumentException(
        "Correct answers must be unique option IDs"
      );
    }
  }

  public String fingerprint(JsonNode snapshot) {
    var optionTexts = new TreeSet<String>();
    snapshot
      .path("options")
      .forEach(option ->
        optionTexts.add(normalize(option.path("text").asText()))
      );
    return LocalAuthService.hash(
      normalize(snapshot.path("prompt").asText()) +
      "|" +
      optionTexts +
      "|" +
      snapshot.path("imageAssetId").asText()
    );
  }

  private String normalize(String value) {
    return value.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
  }
}
