package com.ishine.ishinerest.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ishine.ishinerest.util.StudentAnswerDeserializer;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore
    private Question question;

    @Column(name = "question_id", insertable = false, updatable = false)
    private Long questionId;

    @Column(nullable = true)
    @JsonDeserialize(using = StudentAnswerDeserializer.class)
    private String studentAnswer;

    @Column(nullable = false)
    private Boolean isCorrect;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnore
    private Student student;

    @Column(name = "student_id", insertable = false, updatable = false)
    private Long studentId;

    @Column(nullable = false)
    private Integer attemptCount = 1; // Default 1 for new attempts

    // Transient fields to accept subjectId and chapterId from JSON payload but not persist them
    @Transient
    private String subjectId;
    
    @Transient
    private String chapterId;
}