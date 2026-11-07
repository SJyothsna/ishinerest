package com.ishine.ishinerest.pojo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class PracticeSubmitResponse {

    private String sessionId;
    private String chapterId;
    private String status;
    private int totalQuestions;
    private int answeredCount;
    private int correctCount;
    private int incorrectCount;
    private double scorePercentage;
    private LocalDateTime submittedAt;
    /** true when this session id had already been submitted and the stored result is returned */
    private boolean alreadySubmitted;
    private List<Result> results = new ArrayList<>();

    @Data
    public static class Result {
        private Long questionId;
        private Integer attemptOrder;
        private boolean answered;
        /** null when unanswered */
        private Boolean isCorrect;
    }
}
