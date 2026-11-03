package com.istudy.config;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrors {

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<?> status(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode()).body(
      Map.of(
        "message",
        e.getReason() == null ? "Request could not be completed" : e.getReason()
      )
    );
  }

  @ExceptionHandler(
    org.springframework.web.bind.MethodArgumentNotValidException.class
  )
  ResponseEntity<?> validation(
    org.springframework.web.bind.MethodArgumentNotValidException e
  ) {
    return ResponseEntity.badRequest()
      .body(
        Map.of(
          "message",
          "Check the required fields and their allowed lengths."
        )
      );
  }

  @ExceptionHandler(
    org.springframework.web.multipart.MaxUploadSizeExceededException.class
  )
  ResponseEntity<?> uploadTooLarge() {
    return ResponseEntity.status(413).body(
      Map.of(
        "message",
        "This file is too large. Textbooks must be under 50 MB; images under 10 MB."
      )
    );
  }

  @ExceptionHandler(
    org.springframework.http.converter.HttpMessageNotReadableException.class
  )
  ResponseEntity<?> malformed() {
    return ResponseEntity.badRequest()
      .body(Map.of("message", "The request format is invalid."));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<?> invalid(IllegalArgumentException e) {
    return ResponseEntity.badRequest()
      .body(Map.of("message", "Invalid request value"));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<?> conflict() {
    return ResponseEntity.status(409).body(
      Map.of(
        "message",
        "This change conflicts with the current learning records."
      )
    );
  }
}
