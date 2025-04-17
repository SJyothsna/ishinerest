package com.ishine.ishinerest.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "prev_exam_papers")
public class PrevExamPaper {

    @Id
    @Column(name = "exam_id")
    private String examId;

    @Column(name = "exam_year")
    private Integer examYear;

    @Column(name = "paperType")
    private String paperType;

    @Column(name = "title")
    private String title;

    @Column(name = "subject_id")
    private String subjectId;

    @Column(name = "question_paper")
    private String questionPaper;

    @Column(name = "marking_scheme")
    private String markingScheme;

    @Column(name = "level")
    private String level;
}
