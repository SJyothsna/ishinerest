package com.ishine.ishinerest.pojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for updating question visibility
 */
public record UpdateQuestionVisibilityRequest(
    @NotBlank 
    @Pattern(regexp = "PUBLIC|PRIVATE|SHARED", message = "Visibility must be PUBLIC, PRIVATE, or SHARED")
    String visibility
) {}

// Made with Bob