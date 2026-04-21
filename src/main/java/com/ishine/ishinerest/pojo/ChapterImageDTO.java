package com.ishine.ishinerest.pojo;

import java.time.LocalDateTime;

/**
 * DTO for chapter image metadata
 */
public class ChapterImageDTO {
    private String filename;
    private String studentId;
    private String studentFolder;
    private String url;
    private long size;
    private LocalDateTime createdAt;

    public ChapterImageDTO() {
    }

    public ChapterImageDTO(String filename, String studentId, String studentFolder,
            String url, long size, LocalDateTime createdAt) {
        this.filename = filename;
        this.studentId = studentId;
        this.studentFolder = studentFolder;
        this.url = url;
        this.size = size;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentFolder() {
        return studentFolder;
    }

    public void setStudentFolder(String studentFolder) {
        this.studentFolder = studentFolder;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

// Made with Bob
