package com.ishine.ishinerest.questiongeneration.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One question pattern detected by the AI during material analysis.
 * Returned in AnalyseMaterialResponse; used by the admin to pick which types to generate.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetectedQuestionPattern {

    /** Stable short key used in GenerateQuestionsRequest, e.g. "COUNTING_PRINCIPLE". */
    private String id;

    /** Human-readable label shown in the UI, e.g. "Fundamental Principle of Counting". */
    private String name;

    /** One-sentence description of the question pattern. */
    private String description;

    /** Suggested difficulty level — must be a value present in the questions.difficulty_level column. */
    private String suggestedDifficulty;

    /** True if the AI believes questions of this type typically require a diagram/image. */
    private boolean requiresVisual;
}
