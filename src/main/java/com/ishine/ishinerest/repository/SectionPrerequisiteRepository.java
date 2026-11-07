package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.SectionPrerequisite;
import com.ishine.ishinerest.entity.SectionPrerequisiteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionPrerequisiteRepository extends JpaRepository<SectionPrerequisite, SectionPrerequisiteId> {

    List<SectionPrerequisite> findByIdSectionId(String sectionId);

    void deleteByIdSectionId(String sectionId);

    void deleteByIdPrerequisiteSectionId(String prerequisiteSectionId);
}
