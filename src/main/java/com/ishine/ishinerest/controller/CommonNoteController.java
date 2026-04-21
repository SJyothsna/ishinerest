package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.entity.CommonNote;
import com.ishine.ishinerest.service.CommonNoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/common-notes")
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:5173" }, allowCredentials = "true")
public class CommonNoteController {

    @Autowired
    private CommonNoteService commonNoteService;

    /**
     * Get all active common notes for a specific chapter
     * GET /common-notes/chapter/{chapterId}
     */
    @GetMapping("/chapter/{chapterId}")
    public ResponseEntity<Map<String, Object>> getNotesByChapter(@PathVariable String chapterId) {
        List<CommonNote> notes = commonNoteService.getActiveNotesByChapter(chapterId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", notes);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all common notes (for admin)
     * GET /common-notes
     */
    @GetMapping
    public ResponseEntity<List<CommonNote>> getAllNotes() {
        return ResponseEntity.ok(commonNoteService.getAllNotes());
    }

    /**
     * Get a specific common note by ID
     * GET /common-notes/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommonNote> getNoteById(@PathVariable Long id) {
        return commonNoteService.getNoteById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new common note
     * POST /common-notes
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createNote(@RequestBody CommonNote note) {
        CommonNote createdNote = commonNoteService.createNote(note);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Common note created successfully");
        response.put("data", createdNote);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing common note
     * PUT /common-notes/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateNote(
            @PathVariable Long id,
            @RequestBody CommonNote noteDetails) {
        try {
            CommonNote updatedNote = commonNoteService.updateNote(id, noteDetails);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Common note updated successfully");
            response.put("data", updatedNote);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Delete a common note
     * DELETE /common-notes/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteNote(@PathVariable Long id) {
        try {
            commonNoteService.deleteNote(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Common note deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to delete common note");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Deactivate a common note (soft delete)
     * PATCH /common-notes/{id}/deactivate
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateNote(@PathVariable Long id) {
        try {
            CommonNote deactivatedNote = commonNoteService.deactivateNote(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Common note deactivated successfully");
            response.put("data", deactivatedNote);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    /**
     * Get all distinct categories
     * GET /common-notes/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getAllCategories() {
        List<String> categories = commonNoteService.getAllCategories();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", categories);
        return ResponseEntity.ok(response);
    }
}

// Made with Bob
