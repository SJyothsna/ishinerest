package com.ishine.ishinerest.questiongeneration.service;

import com.ishine.ishinerest.entity.Chapter;
import com.ishine.ishinerest.entity.Question;
import com.ishine.ishinerest.questiongeneration.ai.AiQuestionClient;
import com.ishine.ishinerest.questiongeneration.dto.GenerateQuestionsRequest;
import com.ishine.ishinerest.questiongeneration.dto.GenerateQuestionsResponse;
import com.ishine.ishinerest.questiongeneration.dto.GeneratedQuestionDto;
import com.ishine.ishinerest.questiongeneration.dto.RegenerateQuestionRequest;
import com.ishine.ishinerest.questiongeneration.validation.GeneratedQuestionValidator;
import com.ishine.ishinerest.repository.ChapterRepository;
import com.ishine.ishinerest.service.QuestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles question generation and the final save step.
 *
 * Flow:
 *   images + GenerateQuestionsRequest → AI → GeneratedQuestionDto list (validated)
 *   Admin reviews/edits in UI
 *   Admin confirms → toEntity() → QuestionService.saveQuestions()
 */
@Service
public class QuestionGenerationService {

    private static final Logger log = LoggerFactory.getLogger(QuestionGenerationService.class);

    private final AiQuestionClient aiClient;
    private final GeneratedQuestionValidator validator;
    private final ChapterRepository chapterRepository;
    private final QuestionService questionService;

    public QuestionGenerationService(
            AiQuestionClient aiClient,
            GeneratedQuestionValidator validator,
            ChapterRepository chapterRepository,
            QuestionService questionService
    ) {
        this.aiClient = aiClient;
        this.validator = validator;
        this.chapterRepository = chapterRepository;
        this.questionService = questionService;
    }

    // ─── Generate ─────────────────────────────────────────────────────────────

    /**
     * Generate a batch of questions from reference images + admin selections.
     * Questions are validated before being returned — invalid ones carry a validationError.
     * Nothing is persisted here.
     */
    public GenerateQuestionsResponse generate(
            GenerateQuestionsRequest request,
            List<MultipartFile> images
    ) {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("Images are required for question generation.");
        }
        if (request.getPatterns() == null || request.getPatterns().isEmpty()) {
            throw new IllegalArgumentException("At least one pattern must be selected.");
        }

        log.info("Generating questions for chapterId={}, patterns={}",
                request.getChapterId(),
                request.getPatterns().stream()
                        .map(p -> p.getPatternName() + "×" + p.getCount())
                        .collect(Collectors.joining(", ")));

        GenerateQuestionsResponse response = aiClient.generate(request, images);
        validator.validateAll(response.getQuestions());
        return response;
    }

    // ─── Regenerate single ────────────────────────────────────────────────────

    /**
     * Regenerate a single question without touching the rest of the batch.
     */
    public GeneratedQuestionDto regenerate(RegenerateQuestionRequest req) {
        // Build a minimal context object for the AI client
        GenerateQuestionsRequest context = new GenerateQuestionsRequest();
        context.setSubjectId(req.getSubjectId());
        context.setChapterId(req.getChapterId());
        context.setSectionId(req.getSectionId());
        context.setUsageType(req.getUsageType());
        context.setQuestionSet(req.getQuestionSet());

        GeneratedQuestionDto dto = aiClient.regenerate(
                req.getPatternName(),
                req.getDifficulty(),
                req.getPreviousQuestionText(),
                context
        );

        // Validate the single regenerated question in-place
        validator.validateAll(List.of(dto));
        return dto;
    }

    // ─── Save ─────────────────────────────────────────────────────────────────

    /**
     * Convert the admin-approved draft DTOs to entities and persist via the existing QuestionService.
     * Only valid questions are saved — questions with a validationError are silently skipped
     * (the admin should have removed them in the UI before confirming).
     *
     * @param dtos      The confirmed, potentially edited question drafts from the React UI
     * @param chapterId The chapter to attach all questions to
     * @return Saved Question entities
     */
    public List<Question> saveApprovedQuestions(List<GeneratedQuestionDto> dtos, String chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new IllegalArgumentException("Chapter not found: " + chapterId));

        // Re-run validation on the final (potentially admin-edited) state
        validator.validateAll(dtos);

        List<Question> entities = dtos.stream()
                .filter(dto -> dto.getValidationError() == null)
                .map(dto -> validator.toEntity(dto, chapter))
                .collect(Collectors.toList());

        if (entities.isEmpty()) {
            throw new IllegalArgumentException("No valid questions to save.");
        }

        log.info("Saving {} AI-generated question(s) to chapterId={}", entities.size(), chapterId);
        return questionService.saveQuestions(entities);
    }
}
