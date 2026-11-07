package com.ishine.ishinerest.questiongeneration.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Response from POST /ai/generate/analyse.
 * Contains the question patterns the AI detected in the uploaded reference images.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnalyseMaterialResponse {

    private List<DetectedQuestionPattern> patterns;
}
