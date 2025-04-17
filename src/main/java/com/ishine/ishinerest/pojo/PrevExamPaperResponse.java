package com.ishine.ishinerest.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PrevExamPaperResponse {
    private String subjectId;
    private int year;
    private String paperType;
    private String title;
    private String examUrl;
    private String markingUrl;
}
