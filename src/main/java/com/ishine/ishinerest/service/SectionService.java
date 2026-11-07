package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.Chapter;
import com.ishine.ishinerest.entity.Section;
import com.ishine.ishinerest.entity.SectionPrerequisite;
import com.ishine.ishinerest.entity.SectionPrerequisiteId;
import com.ishine.ishinerest.repository.ChapterRepository;
import com.ishine.ishinerest.repository.SectionPrerequisiteRepository;
import com.ishine.ishinerest.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SectionService {

    private final SectionRepository sectionRepository;
    private final ChapterRepository chapterRepository;
    private final SectionPrerequisiteRepository sectionPrerequisiteRepository;

    public List<Section> getSectionsByChapter(String chapterId) {
        return sectionRepository.findByChapter_ChapterIdOrderByDisplayOrderAscSectionIdAsc(chapterId);
    }

    public List<Section> getAllSections() {
        return sectionRepository.findAll();
    }

    public Section getById(String sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found: " + sectionId));
    }

    public Section saveSection(Section section) {
        // Resolve the chapter from DB so the FK is valid
        Chapter chapter = chapterRepository.findById(section.getChapter().getChapterId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Chapter not found: " + section.getChapter().getChapterId()));
        section.setChapter(chapter);
        return sectionRepository.save(section);
    }

    public List<Section> saveSections(List<Section> sections) {
        return sections.stream().map(this::saveSection).toList();
    }

    public Section updateSection(String sectionId, Section details) {
        Section existing = getById(sectionId);
        existing.setSectionName(details.getSectionName());
        existing.setComplexityLevel(details.getComplexityLevel());
        existing.setDisplayOrder(details.getDisplayOrder() != null ? details.getDisplayOrder() : existing.getDisplayOrder());
        if (details.getChapter() != null && details.getChapter().getChapterId() != null) {
            Chapter chapter = chapterRepository.findById(details.getChapter().getChapterId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Chapter not found: " + details.getChapter().getChapterId()));
            existing.setChapter(chapter);
        }
        return sectionRepository.save(existing);
    }

    @Transactional
    public void deleteSection(String sectionId) {
        if (!sectionRepository.existsById(sectionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found: " + sectionId);
        }
        sectionPrerequisiteRepository.deleteByIdSectionId(sectionId);
        sectionPrerequisiteRepository.deleteByIdPrerequisiteSectionId(sectionId);
        sectionRepository.deleteById(sectionId);
    }

    public List<Section> getPrerequisites(String sectionId) {
        getById(sectionId);
        List<String> prerequisiteIds = sectionPrerequisiteRepository.findByIdSectionId(sectionId).stream()
                .map(p -> p.getId().getPrerequisiteSectionId())
                .toList();
        return sectionRepository.findAllById(prerequisiteIds);
    }

    /** Replaces the full set of prerequisites for a section. */
    @Transactional
    public List<Section> setPrerequisites(String sectionId, List<String> prerequisiteIds) {
        Section section = getById(sectionId);
        Set<String> ids = new LinkedHashSet<>(prerequisiteIds);
        if (ids.contains(sectionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A section cannot be its own prerequisite");
        }
        List<Section> prerequisites = sectionRepository.findAllById(ids);
        if (prerequisites.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more prerequisite sections not found");
        }
        sectionPrerequisiteRepository.deleteByIdSectionId(sectionId);
        sectionPrerequisiteRepository.flush();
        for (Section prerequisite : prerequisites) {
            SectionPrerequisite link = new SectionPrerequisite();
            link.setId(new SectionPrerequisiteId(sectionId, prerequisite.getSectionId()));
            link.setSection(section);
            link.setPrerequisiteSection(prerequisite);
            sectionPrerequisiteRepository.save(link);
        }
        return prerequisites;
    }
}
