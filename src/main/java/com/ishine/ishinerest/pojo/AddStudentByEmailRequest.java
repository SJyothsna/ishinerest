package com.ishine.ishinerest.pojo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for teacher add-or-link student flow.
 */
public record AddStudentByEmailRequest(
    @NotBlank String name,
    @NotBlank @Email String email
) {}

// Made with Bob