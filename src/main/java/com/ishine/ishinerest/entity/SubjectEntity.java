package com.ishine.ishinerest.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "subjects")
public class SubjectEntity {

    @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String subjectId;

    @Column(nullable = false)
    private String subjectName;

    @ManyToMany(mappedBy = "subjects", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<ClassEntity> classes = new HashSet<>();
    //
    // @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, fetch =
    // FetchType.LAZY)
    // @JsonManagedReference(value = "subject-chapter")
    // private List<Chapter> chapters;

}
