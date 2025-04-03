package com.ishine.ishinerest.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "exampapers")
public class ExamPaper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long examId;

    @Column(nullable = false)
    private Integer examYear;  // The year of the exam

    @Column(nullable = false)
    private String level;  // Higher or Lower level

    @Column(nullable = false)
    private String type;  // type of the paper like mock or state

    @Column(nullable = false)
    private String subjectId;  // Section (e.g., Maths, Chemistry, etc.)

    // One exam paper can have many exam questions
    @OneToMany(mappedBy = "examPaper", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ExamQuestion> examQuestions;
}

