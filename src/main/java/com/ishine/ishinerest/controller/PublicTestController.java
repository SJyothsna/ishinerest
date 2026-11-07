package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.pojo.TeacherTestResponseDTO;
import com.ishine.ishinerest.service.TeacherTestService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Public controller for accessing published tests without authentication
 */
@RestController
@RequestMapping("/tests")
@RequiredArgsConstructor
@Tag(name = "Public Tests", description = "Public access to published tests without authentication")
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