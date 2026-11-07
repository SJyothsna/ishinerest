package com.ishine.ishinerest.questiongeneration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ishine.ishinerest.entity.Question;
import com.ishine.ishinerest.questiongeneration.ai.AiGenerationException;
import com.ishine.ishinerest.questiongeneration.dto.AnalyseMaterialResponse;
import com.ishine.ishinerest.questiongeneration.dto.GenerateQuestionsRequest;
import com.ishine.ishinerest.questiongeneration.dto.GenerateQuestionsResponse;
import com.ishine.ishinerest.questiongeneration.dto.GeneratedQuestionDto;
import com.ishine.ishinerest.questiongeneration.dto.RegenerateQuestionRequest;
import com.ishine.ishinerest.questiongeneration.service.MaterialAnalysisService;
import com.ishine.ishinerest.questiongeneration.service.QuestionGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * AI Question Generation endpoints.
 *
 * POST /ai/generate/analyse     — analyse images, return detected question patterns
 * POST /ai/generate/questions   — generate a batch of questions from images + pattern selections
 * POST /ai/generate/regenerate  — regenerate a single question without rebuilding the batch
 * POST /ai/generate/save        — persist the admin-confirmed draft via existing QuestionService
 */
@RestController
@RequestMapping("/ai/generate")
@CrossOrigin(origins = {
        "http://localhost:3000",
        "http://localhost:3001",
        "http://localhost:5173"
})
@Tag(name = "AI Question Generation", description = "Generate questions from reference images using AI")
public class QuestionGenerationController {

    private static final Logger log = LoggerFactory.getLogger(QuestionGenerationController.class);

    private final MaterialAnalysisService analysisService;
    private final QuestionGenerationService generationService;
    private final ObjectMapper objectMapper;

    public QuestionGenerationController(
            MaterialAnalysisService analysisService,
            QuestionGenerationService generationService,
            ObjectMapper objectMapper
    ) {
        this.analysisService = analysisService;
        this.generationService = generationService;
        this.objectMapper = objectMapper;
    }

    // ─── Step 1: Analyse ──────────────────────────────────────────────────────

    @Operation(
        summary = "Analyse reference images",
        description = "Identify question patterns in uploaded textbook/reference images. " +
                      "Images are forwarded to the AI and then discarded — not stored."
    )
    @PostMapping(value = "/analyse", consumes = "multipart/form-data")
    public ResponseEntity<?> analyse(
            @RequestPart("images") List<MultipartFile> images,
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) String chapterId
    ) {
        try {
            AnalyseMaterialResponse result = analysisService.analyse(images, subjectId, chapterId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (AiGenerationException e) {
            log.error("AI provider error during analyse", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during analyse", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("An unexpected error occurred. Please try again."));
        }
    }

    // ─── Step 2: Generate batch ───────────────────────────────────────────────

    @Operation(
        summary = "Generate questions from reference images",
        description = "Generate a batch of questions based on the selected patterns. " +
                      "Returns the draft for admin review — nothing is saved to the database."
    )
    @PostMapping(value = "/questions", consumes = "multipart/form-data")
    public ResponseEntity<?> generate(
            @RequestPart("images") List<MultipartFile> images,
            @RequestPart("request") String requestJson
    ) {
        try {
            GenerateQuestionsRequest request = objectMapper.readValue(requestJson, GenerateQuestionsRequest.class);
            GenerateQuestionsResponse result = generationService.generate(request, images);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (AiGenerationException e) {
            log.error("AI provider error during generate", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during generate", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("An unexpected error occurred. Please try again."));
        }
    }

    // ─── Step 3: Regenerate single question ───────────────────────────────────

    @Operation(
        summary = "Regenerate a single question",
        description = "Regenerate one question for a given pattern without rebuilding the whole batch."
    )
    @PostMapping("/regenerate")
    public ResponseEntity<?> regenerate(@RequestBody RegenerateQuestionRequest request) {
        try {
            GeneratedQuestionDto result = generationService.regenerate(request);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (AiGenerationException e) {
            log.error("AI provider error during regenerate", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during regenerate", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("An unexpected error occurred. Please try again."));
        }
    }

    // ─── Step 4: Save approved draft ─────────────────────────────────────────

    @Operation(
        summary = "Save approved generated questions",
        description = "Persist the admin-confirmed question draft via the existing QuestionService. " +
                      "This is the only step that writes to the database."
    )
    @PostMapping("/save")
    public ResponseEntity<?> save(
            @RequestBody List<GeneratedQuestionDto> dtos,
            @RequestParam String chapterId
    ) {
        try {
            List<Question> saved = generationService.saveApprovedQuestions(dtos, chapterId);
            return ResponseEntity.ok(Map.of(
                    "saved", saved.size(),
                    "message", saved.size() + " question(s) saved successfully."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during save", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("Failed to save questions. Please try again."));
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Map<String, String> error(String message) {
        return Map.of("error", message);
    }
}
