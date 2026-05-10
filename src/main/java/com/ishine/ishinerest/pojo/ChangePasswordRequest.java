package com.ishine.ishinerest.pojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for changing password
 */
public record ChangePasswordRequest(
    @NotBlank String currentPassword,
    @Size(min = 8) @NotBlank String newPassword
) {}

// Made with Bob