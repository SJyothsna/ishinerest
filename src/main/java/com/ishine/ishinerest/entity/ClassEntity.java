package com.ishine.ishinerest.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "classes")
public class ClassEntity {

    @Id
    @Column(name = "class_id")
    private Integer classId;

    @Column(nullable = false, unique = true)
    private String className;

    @Column(name = "exam")
    private String exam;

    @Column(name = "exam_id")
    private Integer examId;

    @ManyToMany(fetch = FetchType.EAGER, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "class_subjects", joinColumns = @JoinColumn(name = "class_id"), inverseJoinColumns = @JoinColumn(name = "subject_id"))
    private Set<SubjectEntity> subjects = new HashSet<>();
}