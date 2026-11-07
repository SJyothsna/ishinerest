package com.ishine.ishinerest.pojo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PracticeSelectRequest {

    @Min(1)
    @Max(100)
    private int count = 10;

    /** AUTO (default), EASY, MEDIUM or HARD. Basic/Intermediate/Advanced are accepted as aliases. */
    private String difficulty = "AUTO";

    /** PRACTICE (default), TEST, BOTH or ALL. Questions with usage type Both always match. */
    private String usageType = "PRACTICE";

    /** Question set number; null = any set */
    private Integer questionSet;
}
