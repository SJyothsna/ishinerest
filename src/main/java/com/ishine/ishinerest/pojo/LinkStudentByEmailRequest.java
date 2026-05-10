package com.ishine.ishinerest.pojo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for linking a student to a parent by email
 */
public record LinkStudentByEmailRequest(
    @NotBlank(message = "Student email is required")
    @Email(message = "Invalid email format")
    String studentEmail,
    
    String relationshipType  // Optional: MOTHER, FATHER, GUARDIAN, etc.
) {}

// Made with Bob