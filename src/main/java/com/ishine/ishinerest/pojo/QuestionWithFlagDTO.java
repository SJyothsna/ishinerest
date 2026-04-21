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
    private String explanation;
    private String difficultyLevel;
    private String notes;
    private String createdBy;
    private String tags;
    private String sectionId;
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
        this.explanation = question.getExplanation();
        this.difficultyLevel = question.getDifficultyLevel();
        this.notes = question.getNotes();
        this.createdBy = question.getCreatedBy();
        this.tags = question.getTags();
        this.sectionId = question.getSectionId();
        this.isFlagged = isFlagged;
    }
}

// Made with Bob
