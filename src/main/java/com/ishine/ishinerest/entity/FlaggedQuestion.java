package com.ishine.ishinerest.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "flagged_questions", uniqueConstraints = @UniqueConstraint(columnNames = { "student_id", "question_id" }))
public class FlaggedQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "flagged_at", nullable = false)
    private LocalDateTime flaggedAt;

    @PrePersist
    protected void onCreate() {
        flaggedAt = LocalDateTime.now();
    }
}

// Made with Bob
