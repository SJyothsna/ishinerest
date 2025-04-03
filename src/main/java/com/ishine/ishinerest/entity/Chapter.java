package com.ishine.ishinerest.entity;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "chapters")
public class Chapter {

    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String chapterId;

    @Column(nullable = false)
    private String chapterName;

     @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
     @JsonBackReference(value = "subject-chapter")
    private SubjectEntity subject;

}
