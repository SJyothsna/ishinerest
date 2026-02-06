package com.ishine.ishinerest.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class AuthExceptionHandler {
    @ExceptionHandler(EmailInUseException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleEmailInUse(EmailInUseException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    record ErrorResponse(String message) {}
}
