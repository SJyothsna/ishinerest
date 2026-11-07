package com.ishine.ishinerest.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One submitted practice session for a student in a chapter.
 * Rows are only written by the v1 practice submit endpoint.
 */
@Getter
@Setter
@Entity
@Table(name = "practice_sessions", indexes = {
        @Index(name = "idx_practice_sessions_student_chapter", columnList = "student_id, chapter_id")
})
public class PracticeSession {

    public enum Status {
        STARTED,
        SUBMITTED
    }

    @Id
    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "chapter_id", nullable = false)
    private String chapterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.SUBMITTED;

    @Column(nullable = false)
    private Integer totalQuestions;

    @Column(nullable = false)
    private Integer answeredCount;

    @Column(nullable = false)
    private Integer correctCount;

    @Column(nullable = false)
    private Integer incorrectCount;

    /** correctCount / totalQuestions * 100, rounded to 2 decimals */
    @Column(nullable = false)
    private Double scorePercentage;

    @Column
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime submittedAt;
}
