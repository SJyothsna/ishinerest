package com.ishine.ishinerest.pojo;

import com.ishine.ishinerest.entity.Question;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionWithFlagDTO {
    private Long questionId;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String optionE;
    private String optionF;
    private String correctAnswer;
    private String correctAnswers;
    private int questionType;
    private String questionTypeName;
    private String explanation;
    private String difficultyLevel;
    private String notes;
    private String hint;
    private String createdBy;
    private String tags;
    private String sectionId;
    private String questionImageUrl;
    private boolean isFlagged;

    // Constructor from Question entity
    public QuestionWithFlagDTO(Question question, boolean isFlagged) {
        this.questionId = question.getQuestionId();
        this.questionText = question.getQuestionText();
        this.optionA = question.getOptionA();
        this.optionB = question.getOptionB();
        this.optionC = question.getOptionC();
        this.optionD = question.getOptionD();
        this.optionE = question.getOptionE();
        this.optionF = question.getOptionF();
        this.correctAnswer = question.getCorrectAnswer();
        this.correctAnswers = question.getCorrectAnswers();
        this.questionType = question.getQuestionType();
        this.questionTypeName = mapQuestionTypeName(question.getQuestionType());
        this.explanation = question.getExplanation();
        this.difficultyLevel = question.getDifficultyLevel();
        this.notes = question.getNotes();
        this.hint = question.getHint();
        // Handle User object - extract name or email
        this.createdBy = question.getCreatedBy() != null ? question.getCreatedBy().getName() : null;
        this.tags = question.getTags();
        this.sectionId = question.getSectionId();
        this.questionImageUrl = question.getQuestionImageUrl();
        this.isFlagged = isFlagged;
    }

    private String mapQuestionTypeName(int questionType) {
        return switch (questionType) {
            case 1 -> "SINGLE_CHOICE";
            case 2 -> "MULTIPLE_CHOICE";
            case 3 -> "TRUE_FALSE";
            case 4 -> "FILL_IN_THE_BLANK";
            case 5 -> "SHORT_ANSWER";
            default -> "UNKNOWN";
        };
    }
}

// Made with Bob
