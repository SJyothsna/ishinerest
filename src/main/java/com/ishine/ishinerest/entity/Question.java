package com.ishine.ishinerest.entity;


import com.fasterxml.jackson.annotation.JsonBackReference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionId;

    @Column(nullable = false)
    private String questionText;

    @Column
    private String optionA;

    @Column
    private String optionB;

    @Column
    private String optionC;

    @Column
    private String optionD;

    @Column(nullable = false)
    private String correctAnswer;

    @Column
    private int questionType;

    @Column
    private String explanation;

    @Column
    private String difficultyLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    @JsonBackReference(value = "chapter-question")
    private Chapter chapter;

}
