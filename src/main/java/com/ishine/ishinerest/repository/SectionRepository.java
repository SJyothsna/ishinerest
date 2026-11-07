package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public interface SectionRepository extends JpaRepository<Section, String> {

    List<Section> findByChapter_ChapterIdOrderByDisplayOrderAscSectionIdAsc(String chapterId);

    List<Section> findBySectionIdIn(Collection<String> sectionIds);

    boolean existsById(String sectionId);

    @Transactional
    @Modifying
    @Query("DELETE FROM Section s WHERE s.chapter.chapterId = :chapterId")
    void deleteByChapterId(String chapterId);

    /** Convenience: build a sectionId→sectionName map for a set of IDs */
    default Map<String, String> buildNameMap(Collection<String> sectionIds) {
        if (sectionIds == null || sectionIds.isEmpty()) return Map.of();
        return findBySectionIdIn(sectionIds).stream()
                .collect(Collectors.toMap(Section::getSectionId, Section::getSectionName));
    }
}
