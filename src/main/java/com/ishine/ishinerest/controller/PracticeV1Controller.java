package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.pojo.PracticeSelectRequest;
import com.ishine.ishinerest.pojo.PracticeSelectionResponse;
import com.ishine.ishinerest.pojo.PracticeSubmitRequest;
import com.ishine.ishinerest.pojo.PracticeSubmitResponse;
import com.ishine.ishinerest.service.PracticeSelectionService;
import com.ishine.ishinerest.service.PracticeSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Adaptive chapter practice. Sits alongside the existing /questions and
 * /practice-session-details endpoints, which are unchanged.
 */
@RestController
@RequestMapping("/v1/practice")
@RequiredArgsConstructor
@Tag(name = "Practice v1", description = "Adaptive question selection and graded practice submission")
public class PracticeV1Controller {

    private final PracticeSelectionService selectionService;
    private final PracticeSubmissionService submissionService;

    @PostMapping("/chapters/{chapterId}/questions/select")
    @Operation(summary = "Select adaptive practice questions for a chapter",
            description = "Read-only. First-time students get a complexity/difficulty mix; returning students get questions weighted to weak and unseen sections.")
    public PracticeSelectionResponse select(
            @PathVariable String chapterId,
            @Parameter(description = "Student ID", required = true) @RequestParam Long studentId,
            @Valid @RequestBody(required = false) PracticeSelectRequest request) {
        return selectionService.selectQuestions(studentId, chapterId,
                request != null ? request : new PracticeSelectRequest());
    }

    @PostMapping("/chapters/{chapterId}/submit")
    @Operation(summary = "Submit a completed practice session",
            description = "Answers are graded on the server. Re-submitting the same sessionId returns the stored result.")
    public PracticeSubmitResponse submit(
            @PathVariable String chapterId,
            @Parameter(description = "Student ID", required = true) @RequestParam Long studentId,
            @Valid @RequestBody PracticeSubmitRequest request) {
        return submissionService.submit(studentId, chapterId, request);
    }
}
