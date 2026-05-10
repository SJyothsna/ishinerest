package com.ishine.ishinerest.pojo;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO for selecting teacher subjects.
 */
public record TeacherSubjectSelectionRequest(
    @NotEmpty List<String> subjectIds
) {}

// Made with Bob
