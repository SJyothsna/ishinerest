package com.ishine.ishinerest.pojo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for updating user profile
 */
public record UpdateUserProfileRequest(
    @NotBlank String name,
    @Email @NotBlank String email
) {}

// Made with Bob