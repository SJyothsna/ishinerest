package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.entity.Feedback;
import com.ishine.ishinerest.pojo.FeedbackResponse;
import com.ishine.ishinerest.pojo.SubmitFeedbackRequest;
import com.ishine.ishinerest.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling feedback and contact form submissions
 */
@Slf4j
@RestController
@RequestMapping("/feedback")
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:5173" }, allowCredentials = "true")
@RequiredArgsConstructor
@Tag(name = "Feedback", description = "Contact form and feedback submission API")
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * Submit new feedback or contact form
     * POST /feedback
     */
    @PostMapping
    @Operation(summary = "Submit feedback", description = "Submit a new contact form or feedback message")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Feedback submitted successfully",
                    content = @Content(schema = @Schema(implementation = FeedbackResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = FeedbackResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = FeedbackResponse.class)))
    })
    public ResponseEntity<FeedbackResponse> submitFeedback(
            @Valid @RequestBody SubmitFeedbackRequest request,
            HttpServletRequest httpRequest) {
        try {
            // Extract user ID from session if authenticated (optional)
            Long userId = null;
            Object userIdAttr = httpRequest.getSession().getAttribute("userId");
            if (userIdAttr != null) {
                userId = Long.valueOf(userIdAttr.toString());
            }

            Feedback feedback = feedbackService.submitFeedback(request, userId);
            
            log.info("Feedback submitted successfully. ID: {}, Type: {}", 
                    feedback.getId(), feedback.getType());
            
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(FeedbackResponse.success(feedback.getId()));

        } catch (Exception e) {
            log.error("Error submitting feedback", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(FeedbackResponse.error("Failed to submit feedback. Please try again later."));
        }
    }

    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<FeedbackResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        StringBuilder errorMessage = new StringBuilder("Validation error: ");
        
        FieldError firstError = ex.getBindingResult().getFieldErrors().get(0);
        errorMessage.append(firstError.getDefaultMessage());
        
        log.warn("Validation error in feedback submission: {}", errorMessage);
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(FeedbackResponse.error(errorMessage.toString()));
    }
}

// Made with Bob