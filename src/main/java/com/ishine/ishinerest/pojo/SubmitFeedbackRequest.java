package com.ishine.ishinerest.pojo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for submitting feedback or contact form
 */
public record SubmitFeedbackRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(min = 1, max = 255, message = "Email must be between 1 and 255 characters")
        String email,

        @NotBlank(message = "Type is required")
        @Pattern(regexp = "inquiry|feedback|support|other", message = "Type must be one of: inquiry, feedback, support, other")
        String type,

        @NotBlank(message = "Subject is required")
        @Size(min = 1, max = 500, message = "Subject must be between 1 and 500 characters")
        String subject,

        @NotBlank(message = "Message is required")
        @Size(min = 10, message = "Message must be at least 10 characters")
        String message
) {
}

// Made with Bob