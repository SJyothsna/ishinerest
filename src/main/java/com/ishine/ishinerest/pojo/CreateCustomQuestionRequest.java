package com.ishine.ishinerest.pojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating a custom question
 */
public record CreateCustomQuestionRequest(
    @NotBlank String chapterId,
    String questionText,  // Optional if image will be provided
    String optionA,
    String optionB,
    String optionC,
    String optionD,
    String optionE,
    String optionF,
    String correctAnswer,
    String correctAnswers,
    @NotNull Integer questionType,
    String explanation,
    String difficultyLevel,
    String notes,
    String hint,
    String tags,
    String sectionId,
    String usageType
) {}

// Made with Bob