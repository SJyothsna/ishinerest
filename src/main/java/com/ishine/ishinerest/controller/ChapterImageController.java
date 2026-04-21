package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.pojo.ChapterImageDTO;
import com.ishine.ishinerest.service.ChapterImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing chapter images
 */
@RestController
@RequestMapping("/chapters")
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:5173" }, allowCredentials = "true")
public class ChapterImageController {

    @Autowired
    private ChapterImageService chapterImageService;

    /**
     * List all images in a chapter
     * GET /api/chapters/{chapterId}/images
     * 
     * @param chapterId The chapter ID (e.g., "LC5H0102")
     * @return JSON response with list of images
     */
    @GetMapping("/{chapterId}/images")
    public ResponseEntity<Map<String, Object>> listChapterImages(@PathVariable String chapterId) {
        try {
            List<ChapterImageDTO> images = chapterImageService.listChapterImages(chapterId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", images);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // Invalid chapter ID format
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid chapter ID format");
            response.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            // General error
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to list images");
            response.put("error", e.getMessage());

            // Log the error for debugging
            System.err.println("Error listing images for chapter " + chapterId + ": " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

// Made with Bob
