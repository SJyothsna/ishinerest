package com.ishine.ishinerest.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PrevExamQuestionResponse {
    private String chapterId;
    private int year;
    private String paperType;
    private String section;
    private String questionnumber;
    private String question;
    private String markingUrl;
}
