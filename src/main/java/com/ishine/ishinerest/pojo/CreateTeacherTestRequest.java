package com.ishine.ishinerest.pojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateTeacherTestRequest(
        @NotBlank String title,
        String description,
        String subjectId,
        String chapterId,
        Integer durationMinutes,
        @NotNull Boolean isPublished,
        @NotEmpty List<Long> questionIds
) {
}

// Made with Bob
