package com.ishine.ishinerest.service;

import com.ishine.ishinerest.pojo.ChapterImageDTO;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for managing chapter images
 */
@Service
public class ChapterImageService {

    // Base path for uploads (relative to project root)
    private static final String UPLOADS_BASE_PATH = "public/uploads/chapters";

    // Supported image extensions
    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp");

    /**
     * List all images in a chapter across all student folders
     * 
     * @param chapterId The chapter ID (e.g., "LC5H0102")
     * @return List of ChapterImageDTO objects
     */
    public List<ChapterImageDTO> listChapterImages(String chapterId) {
        // Validate chapterId to prevent path traversal attacks
        if (!isValidChapterId(chapterId)) {
            throw new IllegalArgumentException("Invalid chapter ID format");
        }

        List<ChapterImageDTO> allImages = new ArrayList<>();

        // Construct path to chapter's notes folder
        Path notesPath = Paths.get(UPLOADS_BASE_PATH, chapterId, "notes");
        File notesDir = notesPath.toFile();

        // Check if folder exists
        if (!notesDir.exists() || !notesDir.isDirectory()) {
            return allImages; // Return empty list if folder doesn't exist
        }

        // Read all student folders
        File[] studentFolders = notesDir.listFiles(File::isDirectory);

        if (studentFolders == null) {
            return allImages;
        }

        // Process each student folder
        for (File studentFolder : studentFolders) {
            String studentFolderName = studentFolder.getName();

            // Extract student ID from folder name (e.g., "student_4" -> "4")
            String studentId = extractStudentId(studentFolderName);

            // Read all files in student folder
            File[] files = studentFolder.listFiles(File::isFile);

            if (files == null) {
                continue;
            }

            // Process each file
            for (File file : files) {
                String filename = file.getName();

                // Check if it's an image file
                if (isImageFile(filename)) {
                    try {
                        // Get file metadata
                        Path filePath = file.toPath();
                        BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);

                        // Create DTO
                        ChapterImageDTO imageDTO = new ChapterImageDTO();
                        imageDTO.setFilename(filename);
                        imageDTO.setStudentId(studentId);
                        imageDTO.setStudentFolder(studentFolderName);
                        imageDTO.setUrl(String.format("/uploads/chapters/%s/notes/%s/%s",
                                chapterId, studentFolderName, filename));
                        imageDTO.setSize(attrs.size());

                        // Convert file creation time to LocalDateTime
                        Instant instant = attrs.creationTime().toInstant();
                        LocalDateTime createdAt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                        imageDTO.setCreatedAt(createdAt);

                        allImages.add(imageDTO);

                    } catch (IOException e) {
                        // Log error but continue processing other files
                        System.err.println("Error reading file metadata for: " + file.getAbsolutePath());
                        e.printStackTrace();
                    }
                }
            }
        }

        // Sort by creation date (newest first)
        allImages.sort(Comparator.comparing(ChapterImageDTO::getCreatedAt).reversed());

        return allImages;
    }

    /**
     * Save an uploaded image file
     *
     * @param chapterId        The chapter ID
     * @param studentId        The student ID
     * @param imageBytes       The image file bytes
     * @param originalFilename The original filename
     * @return The saved image URL
     * @throws IOException If file save fails
     */
    public String saveImage(String chapterId, Long studentId, byte[] imageBytes, String originalFilename)
            throws IOException {
        // Validate inputs
        if (!isValidChapterId(chapterId)) {
            throw new IllegalArgumentException("Invalid chapter ID format");
        }

        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Image data is empty");
        }

        // Validate file extension
        if (!isImageFile(originalFilename)) {
            throw new IllegalArgumentException("Invalid image file type");
        }

        // Create student folder path
        String studentFolder = "student_" + studentId;
        Path studentPath = Paths.get(UPLOADS_BASE_PATH, chapterId, "notes", studentFolder);

        // Create directories if they don't exist
        Files.createDirectories(studentPath);

        // Use original filename (sanitize it for safety)
        String filename = sanitizeFilename(originalFilename);

        // Save file
        Path filePath = studentPath.resolve(filename);
        Files.write(filePath, imageBytes);

        // Return URL
        return String.format("/uploads/chapters/%s/notes/%s/%s", chapterId, studentFolder, filename);
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".png"; // default
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * Sanitize filename to prevent path traversal and invalid characters
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "image.png";
        }

        // Remove path separators and other dangerous characters
        String sanitized = filename.replaceAll("[/\\\\:*?\"<>|]", "_");

        // Remove leading dots to prevent hidden files
        sanitized = sanitized.replaceAll("^\\.+", "");

        // If filename becomes empty after sanitization, use default
        if (sanitized.isEmpty()) {
            return "image.png";
        }

        return sanitized;
    }

    /**
     * Validate chapter ID to prevent path traversal attacks
     * Only allows alphanumeric characters and underscores
     */
    private boolean isValidChapterId(String chapterId) {
        if (chapterId == null || chapterId.trim().isEmpty()) {
            return false;
        }

        // Pattern: alphanumeric and underscores only, 1-50 characters
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9_]{1,50}$");
        return pattern.matcher(chapterId).matches();
    }

    /**
     * Extract student ID from folder name
     * e.g., "student_4" -> "4"
     */
    private String extractStudentId(String folderName) {
        if (folderName.startsWith("student_")) {
            return folderName.substring(8); // Remove "student_" prefix
        }
        return folderName;
    }

    /**
     * Check if a file is an image based on extension
     */
    private boolean isImageFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }

        String lowerFilename = filename.toLowerCase();
        return IMAGE_EXTENSIONS.stream()
                .anyMatch(lowerFilename::endsWith);
    }

    /**
     * Get the base uploads path (for configuration purposes)
     */
    public String getUploadsBasePath() {
        return UPLOADS_BASE_PATH;
    }
}

// Made with Bob
