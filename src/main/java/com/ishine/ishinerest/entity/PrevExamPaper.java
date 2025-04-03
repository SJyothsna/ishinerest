package com.ishine.ishinerest.entity;
import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "prev_exam_papers")
public class PrevExamPaper {

    @Id
    @Column(name = "exam_id")
    private Integer examId;

    @Column(name = "exam_year")
    private Integer examYear;

    @Column(name = "type")
    private String type;

    @Column(name = "level")
    private String level;

    @Column(name = "subject_id")
    private String subjectId;

}

