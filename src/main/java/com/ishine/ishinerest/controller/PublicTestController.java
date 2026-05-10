package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.pojo.TeacherTestResponseDTO;
import com.ishine.ishinerest.service.TeacherTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Public controller for accessing published tests without authentication
 */
@RestController
@RequestMapping("/tests")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}, allowCredentials = "true")
public class PublicTestController {

    private final TeacherTestService teacherTestService;

    /**
     * Get a published test by ID (public access)
     * GET /tests/{testId}
     */
    @GetMapping("/{testId}")
    public TeacherTestResponseDTO getTestById(@PathVariable Long testId) {
        return teacherTestService.getTestByIdPublic(testId);
    }
}

// Made with Bob