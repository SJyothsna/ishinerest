package com.ishine.ishinerest.pojo;

/**
 * Response DTO for feedback submission
 */
public record FeedbackResponse(
        boolean success,
        String message,
        Long feedbackId
) {
    public static FeedbackResponse success(Long feedbackId) {
        return new FeedbackResponse(
                true,
                "Thank you for your feedback! We'll get back to you soon.",
                feedbackId
        );
    }

    public static FeedbackResponse error(String message) {
        return new FeedbackResponse(
                false,
                message,
                null
        );
    }
}

// Made with Bob