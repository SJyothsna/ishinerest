package com.ishine.ishinerest.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One answer inside a submitted practice session.
 * isCorrect is null when the question was left unanswered.
 */
@Getter
@Setter
@Entity
@Table(name = "practice_session_answers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_practice_session_answer", columnNames = { "session_id", "question_id" })
})
public class PracticeSessionAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long answerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonIgnore
    private PracticeSession session;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    /** Selected answers joined with commas; null when unanswered */
    @Column(columnDefinition = "TEXT")
    private String selectedAnswers;

    /** null = unanswered (not a graded attempt) */
    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column
    private Integer attemptOrder;

    @Column
    private LocalDateTime answeredAt;

    @Column
    private Integer timeTakenSeconds;
}
