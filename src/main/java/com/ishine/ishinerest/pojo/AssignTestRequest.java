package com.ishine.ishinerest.pojo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record AssignTestRequest(
        @NotNull Long testId,
        @NotEmpty List<Long> studentUserIds,
        LocalDateTime dueDate
) {
}

// Made with Bob