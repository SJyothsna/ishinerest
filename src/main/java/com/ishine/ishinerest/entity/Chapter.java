package com.ishine.ishinerest.entity;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "chapters")
public class Chapter {

    @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String chapterId;
    @Column(nullable = false)
    private String chapterName;

    @Column()
    private String sectionId;
    @Column()
    private String sectionName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    @JsonIgnoreProperties({ "chapters", "classes" })
    private SubjectEntity subject;

}
