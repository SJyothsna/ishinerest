package com.ishine.ishinerest.questiongeneration.service;

import com.ishine.ishinerest.questiongeneration.ai.AiQuestionClient;
import com.ishine.ishinerest.questiongeneration.dto.AnalyseMaterialResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Handles the first step: sending reference images to the AI and
 * returning the detected question patterns.
 * The analysis result is never persisted — it is ephemeral UI state.
 */
@Service
public class MaterialAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(MaterialAnalysisService.class);
    private static final int MAX_IMAGES = 10;

    private final AiQuestionClient aiClient;

    public MaterialAnalysisService(AiQuestionClient aiClient) {
        this.aiClient = aiClient;
    }

    /**
     * Analyse the uploaded reference images and return detected question patterns.
     *
     * @param images    Up to MAX_IMAGES reference images
     * @param subjectId Context for the AI prompt
     * @param chapterId Context for the AI prompt
     */
    public AnalyseMaterialResponse analyse(
            List<MultipartFile> images,
            String subjectId,
            String chapterId
    ) {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("At least one image is required.");
        }
        if (images.size() > MAX_IMAGES) {
            throw new IllegalArgumentException(
                    "Maximum " + MAX_IMAGES + " images allowed per analysis request.");
        }

        // Validate each file is an accepted image type
        for (MultipartFile image : images) {
            String ct = image.getContentType();
            if (ct == null || (!ct.equals("image/jpeg") && !ct.equals("image/png"))) {
                throw new IllegalArgumentException(
                        "Unsupported file type '" + ct + "'. Only JPG and PNG are accepted.");
            }
            if (image.getSize() > 20 * 1024 * 1024) { // 20 MB per image
                throw new IllegalArgumentException(
                        "Image '" + image.getOriginalFilename() + "' exceeds the 20 MB size limit.");
            }
        }

        log.info("Analysing {} image(s) for subjectId={}, chapterId={}", images.size(), subjectId, chapterId);
        return aiClient.analyse(images, subjectId, chapterId);
    }
}
