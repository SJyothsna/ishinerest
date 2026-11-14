package com.istudy.learning;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import java.util.*;

public final class LearningModels {

  public record CreateSession(
    @NotNull UUID offeringId,
    UUID chapterId,
    List<UUID> sectionIds,
    @NotBlank String kind,
    @NotBlank String mode,
    @NotBlank String scope,
    @NotBlank String difficulty,
    @Min(1) @Max(100) int questionCount,
    @Min(60) @Max(10800) Integer timerSeconds,
    UUID objectiveId
  ) {
    public CreateSession(
      UUID offeringId, UUID chapterId, List<UUID> sectionIds, String kind,
      String mode, String scope, String difficulty, int questionCount, Integer timerSeconds
    ) {
      this(offeringId, chapterId, sectionIds, kind, mode, scope, difficulty,
        questionCount, timerSeconds, null);
    }
  }

  public record Answer(
    @NotNull UUID sessionQuestionId,
    @NotNull JsonNode response
  ) {}

  public record Submit(
    @NotNull UUID submissionToken,
    @NotNull @Size(max = 100) List<@jakarta.validation.Valid Answer> answers
  ) {}

  public record Drafts(
    @NotNull @Size(max = 100) List<@jakarta.validation.Valid Answer> answers
  ) {}

  public record ReviewFlag(boolean flagged) {}

  public record QuestionIssue(
    @NotBlank @Pattern(
      regexp = "INCORRECT_ANSWER|UNCLEAR_WORDING|MISSING_INFORMATION"
    ) String reason
  ) {}

  public record Candidate(
    UUID questionId,
    UUID revisionId,
    UUID placementId,
    UUID sectionId,
    UUID chapterId,
    JsonNode snapshot,
    int attempts,
    int correct,
    boolean flagged,
    int questionSet,
    boolean seen,
    boolean familySeen,
    UUID objectiveId,
    String familyCode,
    String difficulty
  ) {
    UUID coverageKey() {
      return objectiveId == null ? sectionId : objectiveId;
    }

    String familyKey() {
      return objectiveId == null || familyCode == null
        ? questionId.toString()
        : objectiveId + "/" + familyCode;
    }
  }

  private LearningModels() {}
}
