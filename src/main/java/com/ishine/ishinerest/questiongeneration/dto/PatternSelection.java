package com.ishine.ishinerest.questiongeneration.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One pattern entry in a GenerateQuestionsRequest — which pattern to generate for, and how many.
 */
@Getter
@Setter
@NoArgsConstructor
public class PatternSelection {

    /** Must match DetectedQuestionPattern.id returned from the analyse step. */
    private String patternId;

    /** Human-readable name forwarded to the prompt. */
    private String patternName;

    /** Number of questions to generate for this pattern. */
    private int count;

    /** Desired difficulty level — must match values in questions.difficulty_level. */
    private String difficulty;
}
