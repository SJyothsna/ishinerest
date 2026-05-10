package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.pojo.AssignTestRequest;
import com.ishine.ishinerest.pojo.CreateTeacherTestRequest;
import com.ishine.ishinerest.pojo.TeacherTestResponseDTO;
import com.ishine.ishinerest.pojo.TestAssignmentDTO;
import com.ishine.ishinerest.service.TeacherTestService;
import com.ishine.ishinerest.service.TestAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Generic Test Controller - allows any user (teacher or parent) to create tests and assign them
 */
@RestController
@RequestMapping("/users/{userId}/tests")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}, allowCredentials = "true")
public class TeacherTestController {

    private final TeacherTestService teacherTestService;
    private final TestAssignmentService testAssignmentService;

    /**
     * Create a test for any user (teacher or parent)
     * POST /users/{userId}/tests
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherTestResponseDTO createTest(
            @PathVariable Long userId,
            @Valid @RequestBody CreateTeacherTestRequest request) {
        return teacherTestService.createTeacherTest(userId, request);
    }

    /**
     * Get all tests created by a user
     * GET /users/{userId}/tests
     */
    @GetMapping
    public List<TeacherTestResponseDTO> getUserTests(@PathVariable Long userId) {
        return teacherTestService.getTeacherTests(userId);
    }

    /**
     * Get a specific test by ID for a user
     * GET /users/{userId}/tests/{testId}
     */
    @GetMapping("/{testId}")
    public TeacherTestResponseDTO getTestById(
            @PathVariable Long userId,
            @PathVariable Long testId) {
        return teacherTestService.getTeacherTestById(userId, testId);
    }

    /**
     * Assign a test to one or more students
     * POST /users/{userId}/tests/assign
     */
    @PostMapping("/assign")
    @ResponseStatus(HttpStatus.CREATED)
    public List<TestAssignmentDTO> assignTest(
            @PathVariable Long userId,
            @Valid @RequestBody AssignTestRequest request) {
        return testAssignmentService.assignTest(userId, request);
    }

    /**
     * Get all assignments for a specific test
     * GET /users/{userId}/tests/{testId}/assignments
     */
    @GetMapping("/{testId}/assignments")
    public List<TestAssignmentDTO> getTestAssignments(
            @PathVariable Long userId,
            @PathVariable Long testId) {
        return testAssignmentService.getTestAssignments(userId, testId);
    }

    /**
     * Delete a test assignment
     * DELETE /users/{userId}/tests/assignments/{assignmentId}
     */
    @DeleteMapping("/assignments/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssignment(
            @PathVariable Long userId,
            @PathVariable Long assignmentId) {
        testAssignmentService.deleteAssignment(userId, assignmentId);
    }
}

// Made with Bob
