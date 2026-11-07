package com.ishine.ishinerest.questiongeneration.ai;

import com.ishine.ishinerest.questiongeneration.dto.AnalyseMaterialResponse;
import com.ishine.ishinerest.questiongeneration.dto.GenerateQuestionsRequest;
import com.ishine.ishinerest.questiongeneration.dto.GenerateQuestionsResponse;
import com.ishine.ishinerest.questiongeneration.dto.GeneratedQuestionDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Provider-agnostic interface for AI-powered question generation.
 * Swap the implementation (OpenAI, Gemini, Azure OpenAI) without touching
 * MaterialAnalysisService or QuestionGenerationService.
 */
public interface AiQuestionClient {

    /**
     * Analyse reference images and return detected question patterns.
     * Images are never stored — they are forwarded to the AI and then discarded.
     *
     * @param images    One or more textbook/reference page images
     * @param subjectId Context hint for the AI prompt
     * @param chapterId Context hint for the AI prompt
     */
    AnalyseMaterialResponse analyse(List<MultipartFile> images, String subjectId, String chapterId);

    /**
     * Generate a batch of questions based on the approved pattern selections.
     * Images must be re-supplied because they are not persisted between requests.
     *
     * @param request  The generation request with patterns, counts, and context
     * @param images   The same reference images used in the analyse step
     */
    GenerateQuestionsResponse generate(GenerateQuestionsRequest request, List<MultipartFile> images);

    /**
     * Regenerate a single question for a given pattern without rebuilding the whole batch.
     *
     * @param patternName          The question pattern to generate for
     * @param difficulty           Target difficulty level
     * @param previousQuestionText Text of the question to avoid reproducing
     * @param context              Subject/chapter/section/usageType/questionSet context
     */
    GeneratedQuestionDto regenerate(
            String patternName,
            String difficulty,
            String previousQuestionText,
            GenerateQuestionsRequest context
    );
}
