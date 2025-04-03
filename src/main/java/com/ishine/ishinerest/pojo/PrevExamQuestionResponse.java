package com.ishine.ishinerest.pojo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrevExamQuestionResponse {

    private Integer examId;
    private Integer examYear;
    private String type;
    private String qid;
    private String section;
    private String question;
    private String markingScheme;

    public PrevExamQuestionResponse(Integer examId, Integer examYear, String type,
                                    String qId, String section, String question, String markingScheme) {
        this.examId = examId;
        this.examYear = examYear;
        this.type = type;
        this.qid = "Question " + qId;
        this.section = "Section " + section;
        this.question = question;
        this.markingScheme = markingScheme;
    }

    // Getters and setters
}

