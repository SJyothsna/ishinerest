package com.ishine.ishinerest.entity;


import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "examquestions")
public class ExamQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)  // foreign key to ExamPaper
    @JsonBackReference(value = "exam-paper")
    private ExamPaper examPaper;  // This will be the reference to the exam paper


    @Column(nullable = false)
    private String questionText;

    @Column(nullable = false)
    private String questionType;

    @Column(nullable = false)
    private String solution;

    private String questionImage;

    private String markingSchemeImage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    @JsonBackReference(value = "chapter-examquestion")
    private Chapter chapter;
}
