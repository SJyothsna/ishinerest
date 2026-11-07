package com.ishine.ishinerest.questiongeneration.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Response from POST /ai/generate/questions — the full draft for admin review. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenerateQuestionsResponse {

    private List<GeneratedQuestionDto> questions;
}
