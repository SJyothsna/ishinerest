package com.ishine.ishinerest.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sections")
public class Section {

    @Id
    private String sectionId;

    @Column(nullable = false)
    private String sectionName;

    @Column
    private Integer displayOrder = 0;

    @Column(name = "complexity_level", length = 20)
    private String complexityLevel; // FOUNDATION, INTERMEDIATE, ADVANCED

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "chapter_id", nullable = false)
    @JsonIgnoreProperties({"sections", "subject"})
    private Chapter chapter;
}
