package com.ishine.ishinerest.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "YouTube video record returned by all read and write endpoints")
public class VideoDto {

    @Schema(description = "Auto-generated primary key", example = "1")
    private Long    videoId;

    @Schema(description = "Display title of the video", example = "Introduction to Algebra")
    private String  title;

    @Schema(description = "Full YouTube watch URL", example = "https://www.youtube.com/watch?v=abc123")
    private String  youtubeUrl;

    @Schema(description = "11-character YouTube video ID — use for thumbnail: https://img.youtube.com/vi/{id}/mqdefault.jpg",
            example = "abc123")
    private String  youtubeVideoId;

    @Schema(description = "Optional display duration in minutes", example = "14", nullable = true)
    private Integer durationMinutes;

    @Schema(description = "Sort order within a chapter (lower = shown first)", example = "1")
    private Integer displayOrder;

    @Schema(description = "ID of the parent chapter", example = "CH-001")
    private String  chapterId;

    @Schema(description = "Chapter name — only populated by GET /videos/subject/{subjectId}", example = "Algebra", nullable = true)
    private String  chapterName;

    @Schema(description = "Section ID — null for chapter-wide videos", example = "SEC-045", nullable = true)
    private String  sectionId;

    @Schema(description = "Section name — null for chapter-wide videos", example = "Linear Equations", nullable = true)
    private String  sectionName;
}
