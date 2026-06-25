package com.ishine.ishinerest.pojo;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentPracticeProgressDTO {
    // Main fields (Set 1 only OR Set 1 + Set 2 combined based on set1Complete)
    private Long totalQuestions;
    private Long practicedQuestions;
    private Long correctAnswers;
    private Long incorrectAnswers;
    private Long notPracticed;
    
    // Set 1 specific breakdown
    private Long set1Total;
    private Long set1Practiced;
    private Long set1Correct;
    private Long set1Incorrect;
    private Long set1NotPracticed;
    
    // Set 2 specific breakdown
    private Long set2Total;
    private Long set2Practiced;
    private Long set2Correct;
    private Long set2Incorrect;
    private Long set2NotPracticed;
    
    // Completion flag
    private Boolean set1Complete;
    
    // Constructor for backward compatibility (main fields only)
    public StudentPracticeProgressDTO(Long totalQuestions, Long practicedQuestions,
                                     Long correctAnswers, Long incorrectAnswers, Long notPracticed) {
        this.totalQuestions = totalQuestions;
        this.practicedQuestions = practicedQuestions;
        this.correctAnswers = correctAnswers;
        this.incorrectAnswers = incorrectAnswers;
        this.notPracticed = notPracticed;
    }
}
