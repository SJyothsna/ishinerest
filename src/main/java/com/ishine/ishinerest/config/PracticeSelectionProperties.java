package com.ishine.ishinerest.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Tunable values for adaptive practice question selection.
 * Override in application.properties with the prefix "practice.selection", e.g.
 * practice.selection.needs-practice-below-pct=65
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "practice.selection")
public class PracticeSelectionProperties {

    /** Attempts needed before accuracy is used to rate a section (1..n-1 attempts = LEARNING) */
    private int minAttemptsForRating = 3;
    /** Accuracy below this (with enough attempts) = NEEDS_PRACTICE */
    private double needsPracticeBelowPct = 70;
    /** Accuracy at or above this (with enough attempts) = STRONG; between the two = DEVELOPING */
    private double strongAtOrAbovePct = 80;

    /** A correctly answered question counts as "recent" for this many days */
    private int recentDays = 7;
    /** Max share of a batch that may be previously-incorrect exact repeats */
    private double maxRepeatFraction = 0.2;

    /** First-time student: share of questions per section complexity */
    private int foundationPct = 50;
    private int intermediatePct = 40;
    private int advancedPct = 10;

    /** First-time student: overall difficulty split of the batch */
    private int easyPct = 50;
    private int mediumPct = 40;
    private int hardPct = 10;

    /** Returning student: section weights by status */
    private int weightNeedsPractice = 5;
    private int weightUnseen = 4;
    private int weightLearning = 4;
    private int weightDeveloping = 3;
    private int weightStrong = 1;
}
