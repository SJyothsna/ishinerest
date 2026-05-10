package com.ishine.ishinerest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/question-images")
public class QuestionImageController {

    private static final String BASE_UPLOAD_DIR = "public/uploads/chapters";
    private static final String BASE_URL = "http://localhost:8080/uploads/chapters";

    @PostMapping("/upload")
    public ResponseEntity<?> uploadQuestionImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("chapterId") String chapterId,
            @RequestParam("questionId") Long questionId) {
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "File must be an image"));
            }

            // Create directory structure: public/uploads/chapters/{chapterId}/questions
            String directoryPath = BASE_UPLOAD_DIR + "/" + chapterId + "/questions";
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Generate filename using question ID: q_{questionId}.{ext}
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = "q_" + questionId + fileExtension;

            // Save file
            Path filePath = Paths.get(directoryPath, filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Generate URL
            String imageUrl = BASE_URL + "/" + chapterId + "/questions/" + filename;

            // Return response
            Map<String, String> response = new HashMap<>();
            response.put("questionImageUrl", imageUrl);
            response.put("filename", filename);
            response.put("message", "Image uploaded successfully");

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload image: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteQuestionImage(
            @RequestParam("imageUrl") String imageUrl) {
        
        try {
            // Extract path from URL
            // Example: http://localhost:8080/uploads/chapters/LC5H0102/questions/q_123.png
            // Extract: public/uploads/chapters/LC5H0102/questions/q_123.png
            String relativePath = imageUrl.replace(BASE_URL, BASE_UPLOAD_DIR);
            
            File file = new File(relativePath);
            if (file.exists() && file.delete()) {
                return ResponseEntity.ok(Map.of("message", "Image deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Image not found"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete image: " + e.getMessage()));
        }
    }
}

// Made with Bob
