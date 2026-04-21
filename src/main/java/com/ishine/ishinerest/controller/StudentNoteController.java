package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.entity.StudentNote;
import com.ishine.ishinerest.service.ChapterImageService;
import com.ishine.ishinerest.service.StudentNoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/students")
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:5173" }, allowCredentials = "true")
public class StudentNoteController {

    @Autowired
    private StudentNoteService studentNoteService;

    @Autowired
    private ChapterImageService chapterImageService;

    /**
     * Get all notes for a student and chapter
     * GET /students/{studentId}/notes/{chapterId}
     */
    @GetMapping("/{studentId}/notes/{chapterId}")
    public ResponseEntity<Map<String, Object>> getNotesByStudentAndChapter(
            @PathVariable Long studentId,
            @PathVariable String chapterId) {
        List<StudentNote> notes = studentNoteService.getNotesByStudentAndChapter(studentId, chapterId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", notes);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all notes for a student with optional pagination
     * GET /students/{studentId}/notes
     */
    @GetMapping("/{studentId}/notes")
    public ResponseEntity<Map<String, Object>> getAllNotesByStudent(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String order) {

        Page<StudentNote> notesPage = studentNoteService.getAllNotesByStudentPaginated(
                studentId, limit, offset, sortBy);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", notesPage.getContent());

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("total", notesPage.getTotalElements());
        pagination.put("limit", limit);
        pagination.put("offset", offset);
        pagination.put("hasMore", notesPage.hasNext());
        response.put("pagination", pagination);

        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific note by ID
     * GET /students/{studentId}/notes/note/{noteId}
     */
    @GetMapping("/{studentId}/notes/note/{noteId}")
    public ResponseEntity<Map<String, Object>> getNoteById(
            @PathVariable Long studentId,
            @PathVariable Long noteId) {
        try {
            StudentNote note = studentNoteService.getNoteByIdAndStudent(noteId, studentId)
                    .orElseThrow(() -> new RuntimeException("Note not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", note);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Create a new note
     * POST /students/{studentId}/notes
     */
    @PostMapping("/{studentId}/notes")
    public ResponseEntity<Map<String, Object>> createNote(
            @PathVariable Long studentId,
            @RequestBody StudentNote note) {

        // Validate required fields
        if (note.getTitle() == null || note.getTitle().trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Title is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (note.getChapterId() == null || note.getChapterId().trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Chapter ID is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Set student ID
        note.setStudentId(studentId);

        StudentNote createdNote = studentNoteService.createNote(note);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Note created successfully");
        response.put("data", createdNote);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing note
     * PUT /students/{studentId}/notes/{noteId}
     */
    @PutMapping("/{studentId}/notes/{noteId}")
    public ResponseEntity<Map<String, Object>> updateNote(
            @PathVariable Long studentId,
            @PathVariable Long noteId,
            @RequestBody StudentNote noteDetails) {
        try {
            StudentNote updatedNote = studentNoteService.updateNote(noteId, studentId, noteDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Note updated successfully");
            response.put("data", updatedNote);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            HttpStatus status = e.getMessage().contains("permission") ? HttpStatus.FORBIDDEN : HttpStatus.NOT_FOUND;
            return ResponseEntity.status(status).body(response);
        }
    }

    /**
     * Delete a note
     * DELETE /students/{studentId}/notes/{noteId}
     */
    @DeleteMapping("/{studentId}/notes/{noteId}")
    public ResponseEntity<Map<String, Object>> deleteNote(
            @PathVariable Long studentId,
            @PathVariable Long noteId) {
        try {
            studentNoteService.deleteNote(noteId, studentId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Note deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            HttpStatus status = e.getMessage().contains("permission") ? HttpStatus.FORBIDDEN : HttpStatus.NOT_FOUND;
            return ResponseEntity.status(status).body(response);
        }
    }

    /**
     * Migrate notes from localStorage (bulk create)
     * POST /students/{studentId}/notes/migrate
     */
    @PostMapping("/{studentId}/notes/migrate")
    public ResponseEntity<Map<String, Object>> migrateNotes(
            @PathVariable Long studentId,
            @RequestBody Map<String, List<StudentNote>> request) {

        List<StudentNote> notes = request.get("notes");
        if (notes == null || notes.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "No notes provided for migration");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Set student ID for all notes
        notes.forEach(note -> note.setStudentId(studentId));

        List<StudentNote> createdNotes = studentNoteService.createNotesBulk(notes);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Notes migrated successfully");
        response.put("data", createdNotes);
        response.put("count", createdNotes.size());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Create a note with an image
     * POST /students/{studentId}/notes/with-image
     */
    @PostMapping("/{studentId}/notes/with-image")
    public ResponseEntity<Map<String, Object>> createNoteWithImage(
            @PathVariable Long studentId,
            @RequestParam("image") MultipartFile image,
            @RequestParam("chapterId") String chapterId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "color", defaultValue = "#E1BEE7") String color) {

        try {
            // Validate required fields
            if (chapterId == null || chapterId.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Chapter ID is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            if (image == null || image.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Image file is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Save the image file
            String imageUrl = chapterImageService.saveImage(
                    chapterId,
                    studentId,
                    image.getBytes(),
                    image.getOriginalFilename());

            // Create the note
            StudentNote note = new StudentNote();
            note.setStudentId(studentId);
            note.setChapterId(chapterId);
            note.setTitle(title != null ? title : generateDefaultTitle());
            note.setContent(content);
            note.setImageUrl(imageUrl);
            note.setColor(color);

            StudentNote createdNote = studentNoteService.createNote(note);

            // Prepare response with image info
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Note with image created successfully");

            Map<String, Object> data = new HashMap<>();
            data.put("id", createdNote.getId());
            data.put("studentId", createdNote.getStudentId());
            data.put("chapterId", createdNote.getChapterId());
            data.put("title", createdNote.getTitle());
            data.put("content", createdNote.getContent());
            data.put("color", createdNote.getColor());
            data.put("hasImages", true);
            data.put("imageCount", 1);
            data.put("imageUrl", createdNote.getImageUrl());
            data.put("createdAt", createdNote.getCreatedAt());
            data.put("updatedAt", createdNote.getUpdatedAt());

            response.put("data", data);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to create note with image");
            response.put("error", e.getMessage());

            // Log the error
            System.err.println("Error creating note with image: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Generate default title with timestamp
     */
    private String generateDefaultTitle() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm:ss");
        return "Image Note - " + LocalDateTime.now().format(formatter);
    }

}

// Made with Bob
