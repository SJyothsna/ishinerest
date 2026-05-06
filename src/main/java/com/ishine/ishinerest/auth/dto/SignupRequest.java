package com.ishine.ishinerest.auth.dto;

import com.ishine.ishinerest.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @Size(min = 8) String password,
        @NotNull UserRole role  // STUDENT, PARENT, TEACHER (ADMIN created separately)
) {}
