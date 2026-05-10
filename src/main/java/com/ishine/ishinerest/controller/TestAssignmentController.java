package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.pojo.TestAssignmentDTO;
import com.ishine.ishinerest.service.TestAssignmentService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for students to view and interact with their assigned tests
 */
@RestController
@RequestMapping("/students/{studentUserId}/assigned-tests")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}, allowCredentials = "true")
public class TestAssignmentController {

    private final TestAssignmentService testAssignmentService;

    /**
     * Get all tests assigned to a student
     * GET /students/{studentUserId}/assigned-tests
     */
    @GetMapping
    public List<TestAssignmentDTO> getStudentAssignments(@PathVariable Long studentUserId) {
        return testAssignmentService.getStudentAssignments(studentUserId);
    }

    /**
     * Get a specific assignment by ID
     * GET /students/{studentUserId}/assigned-tests/{assignmentId}
     */
    @GetMapping("/{assignmentId}")
    public TestAssignmentDTO getAssignmentById(
            @PathVariable Long studentUserId,
            @PathVariable Long assignmentId) {
        return testAssignmentService.getAssignmentById(assignmentId);
    }

    /**
     * Start a test (mark as in progress)
     * POST /students/{studentUserId}/assigned-tests/{assignmentId}/start
     */
    @PostMapping("/{assignmentId}/start")
    public TestAssignmentDTO startTest(
            @PathVariable Long studentUserId,
            @PathVariable Long assignmentId) {
        return testAssignmentService.startTest(studentUserId, assignmentId);
    }

    /**
     * Complete a test (submit with score)
     * POST /students/{studentUserId}/assigned-tests/{assignmentId}/complete
     */
    @PostMapping("/{assignmentId}/complete")
    public TestAssignmentDTO completeTest(
            @PathVariable Long studentUserId,
            @PathVariable Long assignmentId,
            @RequestBody CompleteTestRequest request) {
        return testAssignmentService.completeTest(studentUserId, assignmentId, request.score());
    }

    /**
     * Request body for completing a test
     */
    public record CompleteTestRequest(
            @NotNull
            @Min(0)
            @Max(100)
            Integer score
    ) {}
}

// Made with Bob