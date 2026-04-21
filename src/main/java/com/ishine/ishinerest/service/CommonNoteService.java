package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.CommonNote;
import com.ishine.ishinerest.repository.CommonNoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CommonNoteService {

    @Autowired
    private CommonNoteRepository commonNoteRepository;

    /**
     * Get all active common notes for a specific chapter
     */
    public List<CommonNote> getActiveNotesByChapter(String chapterId) {
        return commonNoteRepository.findByChapterIdAndIsActiveTrueOrderByDisplayOrderAsc(chapterId);
    }

    /**
     * Get all common notes for a specific chapter (including inactive)
     */
    public List<CommonNote> getAllNotesByChapter(String chapterId) {
        return commonNoteRepository.findByChapterIdOrderByDisplayOrderAsc(chapterId);
    }

    /**
     * Get all common notes
     */
    public List<CommonNote> getAllNotes() {
        return commonNoteRepository.findAll();
    }

    /**
     * Get a common note by ID
     */
    public Optional<CommonNote> getNoteById(Long id) {
        return commonNoteRepository.findById(id);
    }

    /**
     * Create a new common note
     */
    public CommonNote createNote(CommonNote note) {
        return commonNoteRepository.save(note);
    }

    /**
     * Update an existing common note
     */
    public CommonNote updateNote(Long id, CommonNote noteDetails) {
        CommonNote note = commonNoteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Common note not found with ID: " + id));

        if (noteDetails.getTitle() != null) {
            note.setTitle(noteDetails.getTitle());
        }
        note.setContent(noteDetails.getContent());
        if (noteDetails.getImageUrl() != null) {
            note.setImageUrl(noteDetails.getImageUrl());
        }
        if (noteDetails.getCategory() != null) {
            note.setCategory(noteDetails.getCategory());
        }
        if (noteDetails.getColor() != null) {
            note.setColor(noteDetails.getColor());
        }
        if (noteDetails.getIcon() != null) {
            note.setIcon(noteDetails.getIcon());
        }
        if (noteDetails.getDisplayOrder() != null) {
            note.setDisplayOrder(noteDetails.getDisplayOrder());
        }
        if (noteDetails.getIsActive() != null) {
            note.setIsActive(noteDetails.getIsActive());
        }

        return commonNoteRepository.save(note);
    }

    /**
     * Delete a common note
     */
    public void deleteNote(Long id) {
        commonNoteRepository.deleteById(id);
    }

    /**
     * Soft delete - mark as inactive
     */
    public CommonNote deactivateNote(Long id) {
        CommonNote note = commonNoteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Common note not found with ID: " + id));
        note.setIsActive(false);
        return commonNoteRepository.save(note);
    }

    /**
     * Get all distinct categories
     */
    public List<String> getAllCategories() {
        return commonNoteRepository.findAllDistinctCategories();
    }
}

// Made with Bob
