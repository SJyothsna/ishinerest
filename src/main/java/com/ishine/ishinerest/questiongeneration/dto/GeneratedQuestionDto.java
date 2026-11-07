package com.ishine.ishinerest.questiongeneration.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single AI-generated question in the review draft.
 *
 * questionType int mapping (matches existing questions table):
 *   1 = Choices (MCQ)
 *   2 = Blanks (fill-in-the-blank)
 *
 * All fields map 1:1 to entity.Question — the validator/mapper converts this DTO
 * to entity.Question before calling QuestionService.saveQuestions().
 */
@Getter
@Setter
@NoArgsConstructor
public class GeneratedQuestionDto {

    /** Client-side UUID — used only in the React draft, never persisted. */
    private String draftId;

    private String questionText;

    /**
     * 1 = Choices (MCQ), 2 = Blanks.
     * Must be 1 or 2 — any other value fails validation.
     */
    private int questionType;

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;

    /** Single correct option label, e.g. "B". Used for questionType = 1 (Choices). */
    private String correctAnswer;

    /** For questionType = 2 (Blanks) — the fill-in answer. */
    private String correctAnswerBlanks;

    private String hint;
    private String explanation;

    /**
     * Must match values already present in questions.difficulty_level column.
     * E.g. "Easy", "Medium", "Hard" — confirm against your DB before deploying.
     */
    private String difficultyLevel;

    /** "Practice", "Test", or "Both". */
    private String usageType;

    /** Matches Question.questionSet — "1", "2", etc. */
    private String questionSet;

    /** The pattern this question was generated for — shown as a group header in the UI. */
    private String questionPattern;

    /**
     * True if the AI believes this question ideally requires a supporting diagram.
     * V1: no diagram is generated; admin must attach one manually if needed.
     */
    private boolean requiresImage;

    /** Human-readable description of the required visual, if requiresImage=true. */
    private String visualDescription;

    /** Admin-assigned section ID — set in the review UI, persisted on save. */
    private String sectionId;

    /** Image URL — set by admin in review UI when requiresImage=true. */
    private String questionImageUrl;

    /** Validation error message set by GeneratedQuestionValidator if the question fails. */
    private String validationError;
}
