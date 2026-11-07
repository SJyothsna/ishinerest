package com.ishine.ishinerest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite key for SectionPrerequisite relationship
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SectionPrerequisiteId implements Serializable {

    @Column(name = "section_id")
    private String sectionId;

    @Column(name = "prerequisite_section_id")
    private String prerequisiteSectionId;
}
