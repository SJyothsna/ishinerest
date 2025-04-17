package com.ishine.ishinerest.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private String chapterId;

    @Column(nullable = true)
    private String subjectId;

    @Column(nullable = true)
    private String studentAnswer;

    @Column(nullable = false)
    private Boolean isCorrect;

    @Column(nullable = true)
    private Long studentId;

    @Column(nullable = false)
    private Integer attemptCount = 1;  // Default 1 for new attempts


//    @ManyToOne(fetch = FetchType.LAZY)
//    @JsonIgnore
//    @JoinColumn(name = "student_id", nullable = false)
//    private Student student;
}