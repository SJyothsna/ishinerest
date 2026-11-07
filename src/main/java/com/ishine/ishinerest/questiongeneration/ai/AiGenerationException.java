package com.ishine.ishinerest.questiongeneration.ai;

/**
 * Thrown when the AI provider returns an error or an unparseable response.
 * Caught in QuestionGenerationController and returned as a 502 Bad Gateway.
 */
public class AiGenerationException extends RuntimeException {
    public AiGenerationException(String message) {
        super(message);
    }
}
