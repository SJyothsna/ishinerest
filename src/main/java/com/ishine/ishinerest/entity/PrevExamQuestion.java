package com.ishine.ishinerest.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "prev_exam_questions")
public class PrevExamQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_id")
    private String examId;

    @Column(name = "q_id")
    private String qId;

    @Column(name = "section")
    private String section;

    @Column(name = "chapter_id")
    private String chapterId;

    @Column(name = "question")
    private String question;

    @Column(name = "markingscheme")
    private String markingScheme;
}

