package com.ishine.ishinerest.pojo;

import com.ishine.ishinerest.entity.TeacherTest;
import com.ishine.ishinerest.entity.TeacherTestQuestion;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record TeacherTestResponseDTO(
        Long testId,
        String title,
        String description,
        String subjectId,
        String chapterId,
        Integer durationMinutes,
        Boolean isPublished,
        LocalDateTime createdAt,
        UserDTO createdBy,
        List<QuestionSummaryDTO> questions
) {
    public static TeacherTestResponseDTO fromEntity(TeacherTest teacherTest) {
        List<QuestionSummaryDTO> questions = teacherTest.getTestQuestions().stream()
                .sorted(Comparator.comparing(TeacherTestQuestion::getDisplayOrder))
                .map(testQuestion -> new QuestionSummaryDTO(
                        testQuestion.getQuestion().getQuestionId(),
                        testQuestion.getQuestion().getQuestionText(),
                        testQuestion.getQuestion().getQuestionType(),
                        mapQuestionTypeName(testQuestion.getQuestion().getQuestionType()),
                        testQuestion.getQuestion().getChapter() != null ? testQuestion.getQuestion().getChapter().getChapterId() : null,
                        testQuestion.getQuestion().getChapter() != null && testQuestion.getQuestion().getChapter().getSubject() != null
                                ? testQuestion.getQuestion().getChapter().getSubject().getSubjectId()
                                : null,
                        testQuestion.getDisplayOrder()
                ))
                .toList();

        return new TeacherTestResponseDTO(
                teacherTest.getTestId(),
                teacherTest.getTitle(),
                teacherTest.getDescription(),
                teacherTest.getSubjectId(),
                teacherTest.getChapterId(),
                teacherTest.getDurationMinutes(),
                teacherTest.getIsPublished(),
                teacherTest.getCreatedAt(),
                UserDTO.fromEntity(teacherTest.getCreatedBy()),
                questions
        );
    }

    private static String mapQuestionTypeName(int questionType) {
        return switch (questionType) {
            case 1 -> "SINGLE_CHOICE";
            case 2 -> "MULTIPLE_CHOICE";
            case 3 -> "TRUE_FALSE";
            case 4 -> "FILL_IN_THE_BLANK";
            case 5 -> "SHORT_ANSWER";
            default -> "UNKNOWN";
        };
    }

    public record QuestionSummaryDTO(
            Long questionId,
            String questionText,
            Integer questionType,
            String questionTypeName,
            String chapterId,
            String subjectId,
            Integer displayOrder
    ) {
    }
}

// Made with Bob
