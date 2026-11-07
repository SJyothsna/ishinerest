package com.ishine.ishinerest.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionId;

    @Column(columnDefinition = "TEXT")
    private String questionText;

    @Column
    private String optionA;

    @Column
    private String optionB;

    @Column
    private String optionC;

    @Column
    private String optionD;

    @Column
    private String optionE;

    @Column
    private String optionF;

    @Column
    private String correctAnswer;

    @Column
    private String correctAnswers;

    @Column
    private int questionType;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column
    private String difficultyLevel;
    
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String hint;

    @Column
    private String tags;
    // section number in chapter — FK into the sections table (just the ID stored on questions)
    @Column
    private String sectionId;

    /** Populated at query time — not persisted. Holds the human-readable name for sectionId. */
    @Transient
    private String sectionName;

    @Column
    private String usageType; // Possible values: "Practice", "Test", "Both"

    @Column
    private String questionImageUrl; // URL to the question image

    @Column(length = 20)
    private String questionSet = "1"; // Question set number (1, 2, 3, etc.) for progressive practice

    @Column(name = "question_family_id", length = 100)
    private String questionFamilyId; // Groups questions following the same problem-solving pattern

    @Column(name = "question_style", length = 30)
    private String questionStyle; // DIRECT, APPLICATION, WORD_PROBLEM, REASONING

    // NEW: Creator tracking
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User createdBy;

    @Column(nullable = false)
    private Boolean isCustom = false;

    @Column(length = 50)
    private String visibility = "PUBLIC";  // PUBLIC, PRIVATE, SHARED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    @JsonBackReference(value = "chapter-question")
    private Chapter chapter;

}
