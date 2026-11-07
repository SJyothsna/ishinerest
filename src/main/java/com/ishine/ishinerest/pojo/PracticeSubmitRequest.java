package com.ishine.ishinerest.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PracticeSubmitRequest {

    /** Client-generated unique session id; submitting the same id twice is idempotent */
    @NotBlank
    @Size(max = 100)
    private String sessionId;

    /** Optional: when practice began */
    private LocalDateTime startedAt;

    /** Every question presented. Leave selectedAnswers empty for unanswered questions. */
    @NotEmpty
    @Valid
    private List<Answer> answers;

    @Data
    public static class Answer {
        @NotNull
        private Long questionId;

        /** A JSON array or a single string are both accepted */
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        private List<String> selectedAnswers;

        private Integer attemptOrder;
        private Integer timeTakenSeconds;
    }
}
