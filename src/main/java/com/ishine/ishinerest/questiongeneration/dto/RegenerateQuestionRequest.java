package com.ishine.ishinerest.questiongeneration.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for POST /ai/generate/regenerate — regenerate one specific question
 * without rebuilding the whole batch.
 */
@Getter
@Setter
@NoArgsConstructor
public class RegenerateQuestionRequest {

    private String subjectId;
    private String chapterId;
    private String sectionId;

    /** The pattern/topic this question belongs to. */
    private String patternName;

    /** Desired difficulty for the replacement question. */
    private String difficulty;

    /** The previous question text — the AI must not reproduce it. */
    private String previousQuestionText;

    /** usageType — "Practice", "Test", or "Both". */
    private String usageType;

    /** questionSet — "1", "2", etc. */
    private String questionSet;
}
