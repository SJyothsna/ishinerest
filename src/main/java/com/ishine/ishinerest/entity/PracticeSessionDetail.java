package com.ishine.ishinerest.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "practice_session_details")
public class PracticeSessionDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionDetailId;

    @Column(nullable = false)
    private Long questionId;

    @Column(nullable = true)
    private Long chapterId; // New field for chapter information


    @Column(nullable = true)
    private String studentAnswer;

    @Column(nullable = false)
    private Boolean isCorrect;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
}