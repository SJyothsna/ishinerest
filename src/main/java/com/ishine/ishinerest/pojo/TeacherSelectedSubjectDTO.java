package com.ishine.ishinerest.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TeacherSelectedSubjectDTO {
    private String subjectId;
    private String subjectName;
    private List<ClassSummaryDTO> classes;

    @Data
    @AllArgsConstructor
    public static class ClassSummaryDTO {
        private Integer classId;
        private String className;
        private String exam;
        private Integer examId;
    }
}

// Made with Bob
