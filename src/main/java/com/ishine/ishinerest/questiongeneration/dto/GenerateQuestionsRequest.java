package com.ishine.ishinerest.questiongeneration.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Request body for POST /ai/generate/questions.
 * The images were already uploaded in the analyse step and are re-sent here
 * because we don't persist them between requests (V1 design decision).
 */
@Getter
@Setter
@NoArgsConstructor
public class GenerateQuestionsRequest {

    private String subjectId;
    private String chapterId;

    /** String because Section.sectionId is a String PK. */
    private String sectionId;

    /** usageType value — "Practice", "Test", or "Both". Must match existing DB values. */
    private String usageType;

    /** questionSet value — "1", "2", etc. Matches Question.questionSet. */
    private String questionSet;

    /** Selected patterns with per-pattern count and difficulty. */
    private List<PatternSelection> patterns;

    /** Optional free-text instructions appended to the prompt. */
    private String additionalInstructions;
}
