package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.StudentNote;
import com.ishine.ishinerest.repository.StudentNoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StudentNoteService {

    @Autowired
    private StudentNoteRepository studentNoteRepository;

    /**
     * Get all notes for a student and chapter
     */
    public List<StudentNote> getNotesByStudentAndChapter(Long studentId, String chapterId) {
        return studentNoteRepository.findByStudentIdAndChapterIdOrderByUpdatedAtDesc(studentId, chapterId);
    }

    /**
     * Get all notes for a student
     */
    public List<StudentNote> getAllNotesByStudent(Long studentId) {
        return studentNoteRepository.findByStudentIdOrderByUpdatedAtDesc(studentId);
    }

    /**
     * Get all notes for a student with pagination and sorting
     */
    public Page<StudentNote> getAllNotesByStudentPaginated(Long studentId, int limit, int offset, String sortBy) {
        int page = offset / limit;
        Pageable pageable = PageRequest.of(page, limit);

        if (sortBy != null && !sortBy.isEmpty()) {
            return studentNoteRepository.findByStudentIdWithSorting(studentId, sortBy, pageable);
        }

        return studentNoteRepository.findByStudentIdOrderByUpdatedAtDesc(studentId, pageable);
    }

    /**
     * Get a specific note by ID
     */
    public Optional<StudentNote> getNoteById(Long id) {
        return studentNoteRepository.findById(id);
    }

    /**
     * Get a note by ID and verify ownership
     */
    public Optional<StudentNote> getNoteByIdAndStudent(Long id, Long studentId) {
        return studentNoteRepository.findByIdAndStudentId(id, studentId);
    }

    /**
     * Create a new note
     */
    public StudentNote createNote(StudentNote note) {
        return studentNoteRepository.save(note);
    }

    /**
     * Update an existing note
     */
    public StudentNote updateNote(Long id, Long studentId, StudentNote noteDetails) {
        StudentNote note = studentNoteRepository.findByIdAndStudentId(id, studentId)
                .orElseThrow(() -> new RuntimeException("Note not found or you don't have permission to edit it"));

        if (noteDetails.getTitle() != null) {
            note.setTitle(noteDetails.getTitle());
        }
        note.setContent(noteDetails.getContent());
        if (noteDetails.getImageUrl() != null) {
            note.setImageUrl(noteDetails.getImageUrl());
        }
        if (noteDetails.getColor() != null) {
            note.setColor(noteDetails.getColor());
        }

        return studentNoteRepository.save(note);
    }

    /**
     * Delete a note
     */
    public void deleteNote(Long id, Long studentId) {
        StudentNote note = studentNoteRepository.findByIdAndStudentId(id, studentId)
                .orElseThrow(() -> new RuntimeException("Note not found or you don't have permission to delete it"));
        studentNoteRepository.delete(note);
    }

    /**
     * Get total count of notes for a student
     */
    public long getNotesCount(Long studentId) {
        return studentNoteRepository.countByStudentId(studentId);
    }

    /**
     * Bulk create notes (for migration from localStorage)
     */
    public List<StudentNote> createNotesBulk(List<StudentNote> notes) {
        return studentNoteRepository.saveAll(notes);
    }
}

// Made with Bob
