package com.ishine.ishinerest.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentHomeSummaryDTO {

    /** Number of consecutive days (including today) on which the student practised. */
    private int currentStreak;

    /** Up to 3 most recently practised chapters, newest first. */
    private List<RecentActivityItem> recentActivity;

    /**
     * The next chapter in the subject the student last practised.
     * Null when the student has no practice history or has finished all chapters.
     */
    private RecommendedChapter recommendedNext;

    // ------------------------------------------------------------------

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivityItem {
        private String chapterId;
        private String chapterName;
        private String subjectName;
        /** ISO-8601 datetime of the most recent attempt in that chapter. */
        private LocalDateTime lastPracticed;
        /** Total number of question attempts recorded in that chapter session. */
        private long questionsAnswered;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedChapter {
        private String chapterId;
        private String chapterName;
        private String subjectName;
    }
}
