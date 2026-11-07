package com.ishine.ishinerest.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PracticeSelectionResponse {

    /** FIRST_TIME or RETURNING */
    private String studentType;
    private int requestedCount;
    private int returnedCount;
    private List<QuestionWithFlagDTO> questions = new ArrayList<>();
    private List<SectionPlan> sections = new ArrayList<>();

    @Data
    public static class SectionPlan {
        private String sectionId;
        private String sectionName;
        private int displayOrder;
        private String complexityLevel;
        /** UNSEEN, LEARNING, NEEDS_PRACTICE, DEVELOPING or STRONG */
        private String status;
        private int attempts;
        /** null when there are no attempts */
        private Double accuracy;
        private int eligibleQuestions;
        private int planned;
        private int returned;
    }
}
