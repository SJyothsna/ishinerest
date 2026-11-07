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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Google Gemini implementation of AiQuestionClient.
 *
 * Uses the Gemini generateContent REST API with vision (base64 inline images).
 * Free tier: gemini-2.0-flash — no charge within free-tier quotas (Ireland / EEA eligible).
 * Get your free API key at: https://aistudio.google.com/app/apikey
 *
 * Set the key in application.properties:
 *   ai.gemini.api-key=${GEMINI_API_KEY}
 * or export GEMINI_API_KEY=your_key_here in your shell.
 *
 * Images are base64-encoded inline per request — never stored or logged.
 *
 * To switch to OpenAI: remove @Component here, add @Component to OpenAiQuestionClient.
 */
@Component
public class GeminiQuestionClient implements AiQuestionClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiQuestionClient.class);

    private static final String GEMINI_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final QuestionGenerationPromptBuilder promptBuilder;

    @Value("${ai.gemini.api-key}")
    private String apiKey;

    /** Model name — change in application.properties (ai.gemini.model) without recompiling. */
    @Value("${ai.gemini.model:gemini-2.5-flash}")
    private String model;

    public GeminiQuestionClient(QuestionGenerationPromptBuilder promptBuilder) {
        this.promptBuilder = promptBuilder;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    // ─── Analyse ──────────────────────────────────────────────────────────────

    @Override
    public AnalyseMaterialResponse analyse(
            List<MultipartFile> images, String subjectId, String chapterId) {

        String userPrompt = promptBuilder.buildAnalyseSystemPrompt()
                + "\n\n"
                + promptBuilder.buildAnalyseUserPrompt(subjectId, chapterId);

        String rawJson = callGemini(userPrompt, images);

        try {
            // Gemini often wraps JSON in ```json ... ``` fences — strip them
            String cleaned = fixLatexEscapes(stripFences(rawJson));
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode patternsNode = root.get("patterns");
            List<DetectedQuestionPattern> patterns = new ArrayList<>();
            if (patternsNode != null && patternsNode.isArray()) {
                for (JsonNode n : patternsNode) {
                    patterns.add(new DetectedQuestionPattern(
                            n.path("id").asText(),
                            n.path("name").asText(),
                            n.path("description").asText(),
                            n.path("suggestedDifficulty").asText("Medium"),
                            n.path("requiresVisual").asBoolean(false)
                    ));
                }
            }
            return new AnalyseMaterialResponse(patterns);
        } catch (Exception e) {
            log.error("Failed to parse Gemini analyse response: {}", rawJson, e);
            throw new AiGenerationException(
                    "AI returned an unreadable analysis response. Please retry.");
        }
    }

    // ─── Generate batch ───────────────────────────────────────────────────────

    @Override
    public GenerateQuestionsResponse generate(
            GenerateQuestionsRequest request, List<MultipartFile> images) {

        String userPrompt = promptBuilder.buildGenerateSystemPrompt()
                + "\n\n"
                + promptBuilder.buildGenerateUserPrompt(request);

        String rawJson = callGemini(userPrompt, images);
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
        String userPrompt = promptBuilder.buildRegenerateSystemPrompt()
                + "\n\n"
                + promptBuilder.buildRegenerateUserPrompt(
                        patternName, difficulty, previousQuestionText, context);

        // No images for single regenerate — text-only is sufficient
        String rawJson = callGemini(userPrompt, List.of());
        GenerateQuestionsResponse response = parseQuestionsResponse(rawJson, context);

        if (response.getQuestions() == null || response.getQuestions().isEmpty()) {
            throw new AiGenerationException(
                    "AI returned no question for the regenerate request.");
        }
        return response.getQuestions().get(0);
    }

    // ─── Gemini REST call ─────────────────────────────────────────────────────

    /**
     * Calls the Gemini generateContent endpoint.
     * Combines the text prompt with any inline base64 image parts.
     * Returns the raw text content from the first candidate.
     */
    private String callGemini(String textPrompt, List<MultipartFile> images) {
        String url = String.format(GEMINI_URL_TEMPLATE, model, apiKey);

        // Build the parts list: images first, then the text prompt
        List<Map<String, Object>> parts = new ArrayList<>();
        for (MultipartFile image : images) {
            try {
                String base64Data = Base64.getEncoder().encodeToString(image.getBytes());
                String mimeType = image.getContentType() != null
                        ? image.getContentType() : "image/jpeg";
                parts.add(Map.of(
                        "inlineData", Map.of(
                                "mimeType", mimeType,
                                "data", base64Data
                        )
                ));
            } catch (IOException e) {
                log.warn("Could not read image bytes for '{}', skipping.",
                        image.getOriginalFilename());
            }
        }
        parts.add(Map.of("text", textPrompt));

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", parts)
                ),
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 8192
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<?, ?> response = restTemplate.postForObject(url, entity, Map.class);
            if (response == null) {
                throw new AiGenerationException("Empty response from Gemini API.");
            }

            // candidates[0].content.parts[0].text
            @SuppressWarnings("unchecked")
            List<Map<?, ?>> candidates = (List<Map<?, ?>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                // Check for promptFeedback blockReason
                Map<?, ?> feedback = (Map<?, ?>) response.get("promptFeedback");
                String reason = feedback != null
                        ? String.valueOf(feedback.get("blockReason")) : "unknown";
                throw new AiGenerationException(
                        "Gemini returned no candidates. Block reason: " + reason);
            }

            Map<?, ?> content = (Map<?, ?>) candidates.get(0).get("content");
            @SuppressWarnings("unchecked")
            List<Map<?, ?>> responseParts = (List<Map<?, ?>>) content.get("parts");
            if (responseParts == null || responseParts.isEmpty()) {
                throw new AiGenerationException("Gemini response contained no text parts.");
            }

            return (String) responseParts.get(0).get("text");

        } catch (AiGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini API call failed", e);
            throw new AiGenerationException(
                    "Could not reach the AI provider. Please retry.");
        }
    }

    // ─── Response parsing ─────────────────────────────────────────────────────

    private GenerateQuestionsResponse parseQuestionsResponse(
            String rawJson, GenerateQuestionsRequest context) {
        try {
            String cleaned = fixLatexEscapes(stripFences(rawJson));
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode questionsNode = root.get("questions");
            List<GeneratedQuestionDto> dtos = new ArrayList<>();

            if (questionsNode != null && questionsNode.isArray()) {
                for (JsonNode n : questionsNode) {
                    GeneratedQuestionDto dto = new GeneratedQuestionDto();
                    dto.setDraftId(UUID.randomUUID().toString());
                    dto.setQuestionText(n.path("questionText").asText(null));
                    dto.setQuestionType(n.path("questionType").asInt(1));
                    dto.setOptionA(nullIfBlank(n.path("optionA").asText(null)));
                    dto.setOptionB(nullIfBlank(n.path("optionB").asText(null)));
                    dto.setOptionC(nullIfBlank(n.path("optionC").asText(null)));
                    dto.setOptionD(nullIfBlank(n.path("optionD").asText(null)));
                    dto.setCorrectAnswer(nullIfBlank(n.path("correctAnswer").asText(null)));
                    dto.setCorrectAnswerBlanks(
                            nullIfBlank(n.path("correctAnswerBlanks").asText(null)));
                    dto.setHint(nullIfBlank(n.path("hint").asText(null)));
                    dto.setExplanation(nullIfBlank(n.path("explanation").asText(null)));
                    dto.setDifficultyLevel(n.path("difficultyLevel").asText("Medium"));
                    dto.setUsageType(n.path("usageType").asText(context.getUsageType()));
                    dto.setQuestionSet(n.path("questionSet").asText(context.getQuestionSet()));
                    dto.setQuestionPattern(nullIfBlank(n.path("questionPattern").asText(null)));
                    dto.setRequiresImage(n.path("requiresImage").asBoolean(false));
                    dto.setVisualDescription(
                            nullIfBlank(n.path("visualDescription").asText(null)));
                    dtos.add(dto);
                }
            }
            return new GenerateQuestionsResponse(dtos);
        } catch (Exception e) {
            log.error("Failed to parse Gemini generate response: {}", rawJson, e);
            throw new AiGenerationException(
                    "AI returned an unreadable question response. Please retry.");
        }
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    /**
     * Gemini sometimes wraps JSON output in markdown code fences.
     * Strip ```json ... ``` or ``` ... ``` wrappers if present.
     */
    private String stripFences(String text) {
        if (text == null) return "{}";
        String trimmed = text.strip();
        if (trimmed.startsWith("```")) {
            // Remove opening fence (```json or ```)
            int newline = trimmed.indexOf('\n');
            if (newline != -1) trimmed = trimmed.substring(newline + 1);
            // Remove closing fence
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.lastIndexOf("```")).stripTrailing();
            }
        }
        return trimmed;
    }

    /**
     * Gemini embeds raw LaTeX inside JSON string values, e.g. the blanks
     * underline template which contains backslash-underline and backslash-hspace.
     * Those backslash sequences are not valid JSON escape sequences, so Jackson
     * crashes with "Unexpected character".
     *
     * This method walks the raw JSON character-by-character inside string
     * literals and doubles any backslash that is NOT already a valid JSON
     * escape: backslash-quote, backslash-backslash, backslash-slash,
     * backslash-b/f/n/r/t, or backslash-u followed by exactly 4 hex digits.
     */
    private String fixLatexEscapes(String json) {
        if (json == null) return json;
        StringBuilder sb = new StringBuilder(json.length() + 64);
        boolean inString = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (!inString) {
                sb.append(c);
                if (c == '"') inString = true;
            } else {
                if (c == '\\') {
                    // peek at the next character
                    if (i + 1 < json.length()) {
                        char next = json.charAt(i + 1);
                        if (next == '"' || next == '\\' || next == '/'
                                || next == 'b' || next == 'f'
                                || next == 'n' || next == 'r' || next == 't') {
                            // valid two-char escape — keep as-is
                            sb.append(c);
                        } else if (next == 'u' && i + 5 < json.length()
                                && isHexDigit(json.charAt(i + 2))
                                && isHexDigit(json.charAt(i + 3))
                                && isHexDigit(json.charAt(i + 4))
                                && isHexDigit(json.charAt(i + 5))) {
                            // valid backslash-u + 4 hex digits Unicode escape — keep as-is
                            sb.append(c);
                        } else {
                            // LaTeX or other non-JSON backslash — double it
                            sb.append("\\\\");
                        }
                    } else {
                        sb.append("\\\\");
                    }
                } else {
                    sb.append(c);
                    if (c == '"') inString = false;
                }
            }
        }
        return sb.toString();
    }

    private boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank() || s.equals("null")) ? null : s;
    }
}
