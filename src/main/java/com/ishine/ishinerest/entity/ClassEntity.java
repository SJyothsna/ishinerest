package com.ishine.ishinerest.entity;
import jakarta.persistence.*;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "classes")
public class ClassEntity {

    @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String classId;

    @Column(nullable = false, unique = true)
    private String className;
//
//    @OneToMany(mappedBy = "classEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @JsonManagedReference(value = "class-subject")
//    private List<SubjectEntity> subjects;
}