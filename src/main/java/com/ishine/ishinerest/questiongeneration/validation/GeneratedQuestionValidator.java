package com.ishine.ishinerest.questiongeneration.validation;

import com.ishine.ishinerest.questiongeneration.dto.GeneratedQuestionDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Deterministic validation of AI-generated questions.
 * Invalid questions are flagged with a validationError message — they are not thrown away,
 * so the admin can see them in the review UI and either regenerate or delete them.
 * Invalid questions must NOT be passed to QuestionService.
 */
@Component
public class GeneratedQuestionValidator {

    private static final Set<Integer> VALID_TYPES = Set.of(1, 2);
    private static final Set<String> VALID_USAGE_TYPES = Set.of("Practice", "Test", "Both");
    private static final Set<String> VALID_OPTIONS = Set.of("A", "B", "C", "D");

    /**
     * Validate a list of generated questions in-place.
     * Sets validationError on any question that fails — does not remove from the list.
     */
    public void validateAll(List<GeneratedQuestionDto> questions) {
        for (GeneratedQuestionDto q : questions) {
            String error = validate(q);
            q.setValidationError(error); // null = valid
        }
    }

    /**
     * Validate and map a GeneratedQuestionDto to an entity.Question ready for QuestionService.
     * Throws IllegalArgumentException if invalid — call validateAll() before calling this.
     */
    public com.ishine.ishinerest.entity.Question toEntity(
            GeneratedQuestionDto dto,
            com.ishine.ishinerest.entity.Chapter chapter
    ) {
        if (dto.getValidationError() != null) {
            throw new IllegalArgumentException("Cannot persist an invalid question: " + dto.getValidationError());
        }

        com.ishine.ishinerest.entity.Question q = new com.ishine.ishinerest.entity.Question();
        q.setQuestionText(dto.getQuestionText());
        q.setQuestionType(dto.getQuestionType());
        q.setOptionA(dto.getOptionA());
        q.setOptionB(dto.getOptionB());
        q.setOptionC(dto.getOptionC());
        q.setOptionD(dto.getOptionD());

        // For Choices: correctAnswer is the option label (A/B/C/D)
        // For Blanks:  correctAnswer holds the fill-in value
        if (dto.getQuestionType() == 2) {
            q.setCorrectAnswer(dto.getCorrectAnswerBlanks());
        } else {
            q.setCorrectAnswer(dto.getCorrectAnswer());
        }

        q.setHint(dto.getHint());
        q.setExplanation(dto.getExplanation());
        q.setDifficultyLevel(dto.getDifficultyLevel());
        q.setUsageType(dto.getUsageType());
        q.setQuestionSet(dto.getQuestionSet() != null ? dto.getQuestionSet() : "1");
        q.setSectionId(dto.getSectionId());
        q.setQuestionImageUrl(dto.getQuestionImageUrl());
        q.setChapter(chapter);
        q.setIsCustom(false);
        q.setVisibility("PUBLIC");
        return q;
    }

    // ─── Private ──────────────────────────────────────────────────────────────

    /** Returns null if valid, or an error message string if invalid. */
    private String validate(GeneratedQuestionDto q) {
        if (q.getQuestionText() == null || q.getQuestionText().isBlank()) {
            return "Question text must not be empty.";
        }
        if (!VALID_TYPES.contains(q.getQuestionType())) {
            return "Question type must be 1 (Choices) or 2 (Blanks), got: " + q.getQuestionType();
        }
        if (q.getUsageType() != null && !VALID_USAGE_TYPES.contains(q.getUsageType())) {
            return "usageType must be Practice, Test, or Both.";
        }

        if (q.getQuestionType() == 1) {
            // MCQ — must have A, B, C, D and a valid correct answer
            if (isBlank(q.getOptionA()) || isBlank(q.getOptionB())
                    || isBlank(q.getOptionC()) || isBlank(q.getOptionD())) {
                return "MCQ question must have options A, B, C, and D.";
            }
            if (q.getCorrectAnswer() == null || !VALID_OPTIONS.contains(q.getCorrectAnswer().trim().toUpperCase())) {
                return "MCQ correctAnswer must be A, B, C, or D.";
            }
        } else if (q.getQuestionType() == 2) {
            // Blanks — must have a correct answer
            if (isBlank(q.getCorrectAnswerBlanks())) {
                return "Blanks question must have a correctAnswerBlanks value.";
            }
        }

        return null; // valid
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
