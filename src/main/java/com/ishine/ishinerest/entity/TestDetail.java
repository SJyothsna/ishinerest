package com.ishine.ishinerest.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "test_details")
public class TestDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long testDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = true)
    @JsonIgnore
    private TestAssignment assignment;

    @Column(name = "assignment_id", insertable = false, updatable = false)
    private Long assignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore
    private Question question;

    @Column(name = "question_id", insertable = false, updatable = false)
    private Long questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnore
    private Student student;

    @Column(name = "student_id", insertable = false, updatable = false)
    private Long studentId;

    @Column(nullable = true)
    private String studentAnswer;

    @Column(nullable = false)
    private Boolean isCorrect;

    @Column(nullable = false)
    private Integer attemptCount = 1;
}

// Made with Bob
