package com.istudy.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.UUID;

/** Future OpenAI adapter boundary. Output is the same reviewed bank format as a manual upload. */
public interface QuestionGenerationGateway {
  record SourcePassage(int pageStart, int pageEnd, String text) {}
  record TextbookInput(UUID documentId, List<SourcePassage> passages) {}
  record Request(
    UUID offeringId,
    int questionSet,
    JsonNode catalogueReference,
    JsonNode difficultyPlan,
    TextbookInput textbook,
    int challengeStep
  ) {}
  /** Returns a question-bank 1.0 manifest; it must pass normal validation and human review. */
  ObjectNode generateBank(Request request);
}
