package com.ishine.ishinerest.pojo;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for linking parent/teacher to student
 */
public record LinkStudentRequest(
    @NotNull Long studentUserId
) {}

// Made with Bob