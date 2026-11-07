package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.entity.Section;
import com.ishine.ishinerest.service.SectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sections")
@RequiredArgsConstructor
@Tag(name = "Sections", description = "Section management per chapter")
public class SectionController {

    private final SectionService sectionService;

    @GetMapping("/chapter/{chapterId}")
    @Operation(summary = "Get all sections for a chapter")
    public List<Section> getSectionsByChapter(@PathVariable String chapterId) {
        return sectionService.getSectionsByChapter(chapterId);
    }

    @GetMapping
    @Operation(summary = "Get all sections")
    public List<Section> getAllSections() {
        return sectionService.getAllSections();
    }

    @GetMapping("/{sectionId}")
    @Operation(summary = "Get a section by ID")
    public Section getById(@PathVariable String sectionId) {
        return sectionService.getById(sectionId);
    }

    @PostMapping
    @Operation(summary = "Create a new section")
    public Section createSection(@RequestBody Section section) {
        return sectionService.saveSection(section);
    }

    @PostMapping("/bulk")
    @Operation(summary = "Create multiple sections at once")
    public List<Section> createSections(@RequestBody List<Section> sections) {
        return sectionService.saveSections(sections);
    }

    @PutMapping("/{sectionId}")
    @Operation(summary = "Update a section")
    public Section updateSection(@PathVariable String sectionId, @RequestBody Section section) {
        return sectionService.updateSection(sectionId, section);
    }

    @GetMapping("/{sectionId}/prerequisites")
    @Operation(summary = "Get prerequisite sections of a section")
    public List<Section> getPrerequisites(@PathVariable String sectionId) {
        return sectionService.getPrerequisites(sectionId);
    }

    @PutMapping("/{sectionId}/prerequisites")
    @Operation(summary = "Replace the prerequisite sections of a section", description = "Body is a list of prerequisite section IDs")
    public List<Section> setPrerequisites(@PathVariable String sectionId, @RequestBody List<String> prerequisiteIds) {
        return sectionService.setPrerequisites(sectionId, prerequisiteIds);
    }

    @DeleteMapping("/{sectionId}")
    @Operation(summary = "Delete a section")
    public ResponseEntity<String> deleteSection(@PathVariable String sectionId) {
        sectionService.deleteSection(sectionId);
        return ResponseEntity.ok("Section " + sectionId + " deleted.");
    }
}
