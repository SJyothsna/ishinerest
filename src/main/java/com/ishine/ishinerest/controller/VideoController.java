package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.pojo.VideoDto;
import com.ishine.ishinerest.pojo.VideoRequest;
import com.ishine.ishinerest.service.YoutubeVideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
@Tag(name = "Videos", description = "YouTube video management — read by chapter / subject / section; CRUD for ADMIN and TEACHER roles")
public class VideoController {

    private final YoutubeVideoService videoService;

    // ── Read endpoints ────────────────────────────────────────────────────────

    @GetMapping("/chapter/{chapterId}")
    @Operation(
        summary = "Get videos for a chapter",
        description = "Returns all videos belonging to the given chapter, ordered by `displayOrder` ASC. " +
                      "Used by the ChapterPage Videos tab, PracticeOverview strip (first 3), and TestImproved wrong-answer hint (`videos[0]`). " +
                      "Returns `200 []` when no videos exist — never `404`."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Videos returned (empty array if none exist)",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = VideoDto.class))))
    })
    public List<VideoDto> getByChapter(
            @Parameter(description = "Chapter ID", example = "CH-012", required = true)
            @PathVariable String chapterId) {
        return videoService.getByChapter(chapterId);
    }

    @GetMapping("/subject/{subjectId}")
    @Operation(
        summary = "Get all videos for a subject",
        description = "Returns videos across all chapters of the subject. The response includes `chapterName` so " +
                      "the frontend can group cards into accordion panels without a second request. " +
                      "Ordered by chapter name then `displayOrder`. Returns `200 []` when no videos exist."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Videos returned (empty array if none exist)",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = VideoDto.class))))
    })
    public List<VideoDto> getBySubject(
            @Parameter(description = "Subject ID", example = "SUBJ-05", required = true)
            @PathVariable String subjectId) {
        return videoService.getBySubject(subjectId);
    }

    @GetMapping("/section/{sectionId}")
    @Operation(
        summary = "Get videos for a section",
        description = "Returns videos scoped to a specific topic / section, ordered by `displayOrder` ASC. " +
                      "Reserved for future section-level hints. Returns `200 []` when no videos exist."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Videos returned (empty array if none exist)",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = VideoDto.class))))
    })
    public List<VideoDto> getBySection(
            @Parameter(description = "Section ID", example = "SEC-045", required = true)
            @PathVariable String sectionId) {
        return videoService.getBySection(sectionId);
    }

    // ── Admin / write endpoints ───────────────────────────────────────────────

    @PostMapping
    @Operation(
        summary = "Create a video entry",
        description = "Creates a new YouTube video record linked to a subject, chapter, and optionally a section. " +
                      "Requires ADMIN or TEACHER role."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Video created successfully",
                content = @Content(schema = @Schema(implementation = VideoDto.class))),
        @ApiResponse(responseCode = "400", description = "Missing required fields or referenced subject / chapter / section not found",
                content = @Content(schema = @Schema())),
        @ApiResponse(responseCode = "403", description = "Caller does not have ADMIN or TEACHER role",
                content = @Content(schema = @Schema()))
    })
    public ResponseEntity<VideoDto> create(@Valid @RequestBody VideoRequest request) {
        VideoDto created = videoService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{videoId}")
    @Operation(
        summary = "Update a video entry",
        description = "Updates an existing video. All fields are optional — only supplied fields are changed. " +
                      "Passing `sectionId: null` clears the section association (makes it chapter-wide). " +
                      "Requires ADMIN or TEACHER role."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Video updated successfully",
                content = @Content(schema = @Schema(implementation = VideoDto.class))),
        @ApiResponse(responseCode = "400", description = "Referenced subject / chapter / section not found",
                content = @Content(schema = @Schema())),
        @ApiResponse(responseCode = "403", description = "Caller does not have ADMIN or TEACHER role",
                content = @Content(schema = @Schema())),
        @ApiResponse(responseCode = "404", description = "Video not found for the given videoId",
                content = @Content(schema = @Schema()))
    })
    public VideoDto update(
            @Parameter(description = "ID of the video to update", example = "1", required = true)
            @PathVariable Long videoId,
            @RequestBody VideoRequest request) {
        return videoService.update(videoId, request);
    }

    @DeleteMapping("/{videoId}")
    @Operation(
        summary = "Delete a video entry",
        description = "Permanently removes the video record. Requires ADMIN or TEACHER role."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Video deleted — no response body"),
        @ApiResponse(responseCode = "403", description = "Caller does not have ADMIN or TEACHER role",
                content = @Content(schema = @Schema())),
        @ApiResponse(responseCode = "404", description = "Video not found for the given videoId",
                content = @Content(schema = @Schema()))
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "ID of the video to delete", example = "1", required = true)
            @PathVariable Long videoId) {
        videoService.delete(videoId);
    }
}
