package com.ishine.ishinerest.pojo;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
public class PrevExamQuestionResponse {
    private String chapterId;
    private int year;
    private String paperType;
    private String paperName;
    private String questionnumber;
    private List<String> question;
    private List<String> markingUrl;

    public PrevExamQuestionResponse(
            String chapterId,
            int year,
            String paperType,
            String paperName,
            String questionnumber,
            String questionStr,
            String markingUrlStr
    ) {
        this.chapterId = chapterId;
        this.year = year;
        this.paperType = paperType;
        this.paperName = paperName;
        this.questionnumber = questionnumber;
        this.question = splitByComma(questionStr);
        this.markingUrl = splitByComma(markingUrlStr);
    }

    private List<String> splitByComma(String input) {
        if (input == null || input.isBlank()) return List.of();
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .toList();
    }
}
