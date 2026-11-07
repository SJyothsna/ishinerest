package com.ishine.ishinerest.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request body for creating or updating a YouTube video entry")
public class VideoRequest {

    @NotBlank
    @Schema(description = "Display title of the video", example = "Introduction to Algebra", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank
    @Schema(description = "Full YouTube watch URL (https://www.youtube.com/watch?v=... or https://youtu.be/...)",
            example = "https://www.youtube.com/watch?v=abc123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String youtubeUrl;

    @NotBlank
    @Schema(description = "11-character YouTube video ID extracted from the URL",
            example = "abc123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String youtubeVideoId;

    @Schema(description = "Optional display duration in minutes", example = "14")
    private Integer durationMinutes;

    @Schema(description = "Sort position within the chapter (lower = shown first, defaults to 0)", example = "1")
    private Integer displayOrder;

    @NotNull
    @Schema(description = "Subject this video belongs to", example = "SUBJ-05", requiredMode = Schema.RequiredMode.REQUIRED)
    private String subjectId;

    @NotNull
    @Schema(description = "Chapter this video belongs to", example = "CH-012", requiredMode = Schema.RequiredMode.REQUIRED)
    private String chapterId;

    @Schema(description = "Section this video belongs to — omit or null for chapter-wide videos", example = "SEC-045")
    private String sectionId;
}
