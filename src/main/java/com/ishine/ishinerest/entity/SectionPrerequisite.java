package com.ishine.ishinerest.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing that a section requires another section's concepts as a prerequisite
 */
@Getter
@Setter
@Entity
@Table(name = "section_prerequisites")
public class SectionPrerequisite {

    @EmbeddedId
    private SectionPrerequisiteId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sectionId")
    @JoinColumn(name = "section_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("prerequisiteSectionId")
    @JoinColumn(name = "prerequisite_section_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Section prerequisiteSection;
}
