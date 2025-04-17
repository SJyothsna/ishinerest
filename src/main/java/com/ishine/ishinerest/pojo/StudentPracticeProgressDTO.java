package com.ishine.ishinerest.pojo;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentPracticeProgressDTO {
    private Long totalQuestions;
    private Long practicedQuestions;
    private Long correctAnswers;
    private Long incorrectAnswers;
    private Long notPracticed;
}
