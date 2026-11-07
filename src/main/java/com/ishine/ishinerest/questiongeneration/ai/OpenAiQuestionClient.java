package com.ishine.ishinerest.questiongeneration.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ishine.ishinerest.questiongeneration.dto.AnalyseMaterialResponse;
import com.ishine.ishinerest.questiongeneration.dto.DetectedQuestionPattern;
import com.ishine.ishinerest.questiongeneration.dto.GenerateQuestionsRequest;
import com.ishine.ishinerest.questiongeneration.dto.GenerateQuestionsResponse;
import com.ishine.ishinerest.questiongeneration.dto.GeneratedQuestionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * OpenAI GPT-4o implementation of AiQuestionClient.
 * Uses the Chat Completions API with vision (base64-encoded images).
 * Images are encoded inline — never stored or logged.
 *
 * NOT the active bean — GeminiQuestionClient is the active @Component.
 * To switch back to OpenAI: add @Component here and remove it from GeminiQuestionClient.
 * Set ai.openai.api-key in application.properties or as an env var.
 */
public class OpenAiQuestionClient implements AiQuestionClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiQuestionClient.class);
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final QuestionGenerationPromptBuilder promptBuilder;

    @Value("${ai.openai.api-key}")
    private String apiKey;

    public OpenAiQuestionClient(
            QuestionGenerationPromptBuilder promptBuilder
    ) {
        this.promptBuilder = promptBuilder;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    // ─── Analyse ──────────────────────────────────────────────────────────────

    @Override
    public AnalyseMaterialResponse analyse(List<MultipartFile> images, String subjectId, String chapterId) {
        List<Map<String, Object>> messageContent = buildImageContent(images);
        messageContent.add(textContent(promptBuilder.buildAnalyseUserPrompt(subjectId, chapterId)));

        String rawJson = callOpenAi(promptBuilder.buildAnalyseSystemPrompt(), messageContent);

        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode patternsNode = root.get("patterns");
            List<DetectedQuestionPattern> patterns = new ArrayList<>();
            if (patternsNode != null && patternsNode.isArray()) {
                for (JsonNode n : patternsNode) {
                    DetectedQuestionPattern p = new DetectedQuestionPattern(
                            n.path("id").asText(),
                            n.path("name").asText(),
                            n.path("description").asText(),
                            n.path("suggestedDifficulty").asText("Medium"),
                            n.path("requiresVisual").asBoolean(false)
                    );
                    patterns.add(p);
                }
            }
            return new AnalyseMaterialResponse(patterns);
        } catch (Exception e) {
            log.error("Failed to parse AI analyse response", e);
            throw new AiGenerationException("AI returned an unreadable analysis response. Please retry.");
        }
    }

    // ─── Generate batch ───────────────────────────────────────────────────────

    @Override
    public GenerateQuestionsResponse generate(GenerateQuestionsRequest request, List<MultipartFile> images) {
        List<Map<String, Object>> messageContent = buildImageContent(images);
        messageContent.add(textContent(promptBuilder.buildGenerateUserPrompt(request)));

        String rawJson = callOpenAi(promptBuilder.buildGenerateSystemPrompt(), messageContent);
        return parseQuestionsResponse(rawJson, request);
    }

    // ─── Regenerate single ────────────────────────────────────────────────────

    @Override
    public GeneratedQuestionDto regenerate(
            String patternName,
            String difficulty,
            String previousQuestionText,
            GenerateQuestionsRequest context
    ) {
        String userPrompt = promptBuilder.buildRegenerateUserPrompt(
                patternName, difficulty, previousQuestionText, context);
        List<Map<String, Object>> messageContent = List.of(textContent(userPrompt));

        String rawJson = callOpenAi(promptBuilder.buildRegenerateSystemPrompt(), messageContent);
        GenerateQuestionsResponse response = parseQuestionsResponse(rawJson, context);

        if (response.getQuestions() == null || response.getQuestions().isEmpty()) {
            throw new AiGenerationException("AI returned no question for the regenerate request.");
        }
        return response.getQuestions().get(0);
    }

    // ─── Shared HTTP call ─────────────────────────────────────────────────────

    private String callOpenAi(String systemPrompt, List<Map<String, Object>> userContent) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        List<Map<String, Object>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userContent)
        );

        Map<String, Object> body = new HashMap<>();
        body.put("model", MODEL);
        body.put("messages", messages);
        body.put("max_tokens", 4096);
        body.put("temperature", 0.7);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            Map<?, ?> response = restTemplate.postForObject(OPENAI_URL, entity, Map.class);
            if (response == null) {
                throw new AiGenerationException("Empty response from OpenAI API.");
            }
            @SuppressWarnings("unchecked")
            List<Map<?, ?>> choices = (List<Map<?, ?>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new AiGenerationException("OpenAI returned no choices.");
            }
            Map<?, ?> message = (Map<?, ?>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (AiGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI API call failed", e);
            throw new AiGenerationException("Could not reach the AI provider. Please retry.");
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private List<Map<String, Object>> buildImageContent(List<MultipartFile> images) {
        List<Map<String, Object>> content = new ArrayList<>();
        for (MultipartFile image : images) {
            try {
                String base64 = Base64.getEncoder().encodeToString(image.getBytes());
                String mimeType = image.getContentType() != null ? image.getContentType() : "image/jpeg";
                String dataUrl = "data:" + mimeType + ";base64," + base64;

                Map<String, Object> imageObj = Map.of(
                        "type", "image_url",
                        "image_url", Map.of("url", dataUrl, "detail", "high")
                );
                content.add(imageObj);
            } catch (IOException e) {
                log.warn("Could not read image bytes for file '{}', skipping.", image.getOriginalFilename());
            }
        }
        return content;
    }

    private Map<String, Object> textContent(String text) {
        return Map.of("type", "text", "text", text);
    }

    private GenerateQuestionsResponse parseQuestionsResponse(String rawJson, GenerateQuestionsRequest context) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode questionsNode = root.get("questions");
            List<GeneratedQuestionDto> dtos = new ArrayList<>();

            if (questionsNode != null && questionsNode.isArray()) {
                for (JsonNode n : questionsNode) {
                    GeneratedQuestionDto dto = new GeneratedQuestionDto();
                    dto.setDraftId(UUID.randomUUID().toString());
                    dto.setQuestionText(n.path("questionText").asText(null));
                    dto.setQuestionType(n.path("questionType").asInt(1));
                    dto.setOptionA(n.path("optionA").asText(null));
                    dto.setOptionB(n.path("optionB").asText(null));
                    dto.setOptionC(n.path("optionC").asText(null));
                    dto.setOptionD(n.path("optionD").asText(null));
                    dto.setCorrectAnswer(n.path("correctAnswer").asText(null));
                    dto.setCorrectAnswerBlanks(n.path("correctAnswerBlanks").asText(null));
                    dto.setHint(n.path("hint").asText(null));
                    dto.setExplanation(n.path("explanation").asText(null));
                    dto.setDifficultyLevel(n.path("difficultyLevel").asText("Medium"));
                    dto.setUsageType(n.path("usageType").asText(context.getUsageType()));
                    dto.setQuestionSet(n.path("questionSet").asText(context.getQuestionSet()));
                    dto.setQuestionPattern(n.path("questionPattern").asText(null));
                    dto.setRequiresImage(n.path("requiresImage").asBoolean(false));
                    dto.setVisualDescription(n.path("visualDescription").asText(null));
                    dtos.add(dto);
                }
            }
            return new GenerateQuestionsResponse(dtos);
        } catch (Exception e) {
            log.error("Failed to parse AI generate response", e);
            throw new AiGenerationException("AI returned an unreadable question response. Please retry.");
        }
    }
}
