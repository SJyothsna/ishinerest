package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.Chapter;
import com.ishine.ishinerest.entity.Question;
import com.ishine.ishinerest.pojo.QuestionWithFlagDTO;
import com.ishine.ishinerest.repository.ChapterRepository;
import com.ishine.ishinerest.repository.FlaggedQuestionRepository;
import com.ishine.ishinerest.repository.QuestionRepository;
import com.ishine.ishinerest.repository.PracticeSessionDetailRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    @Autowired
    private PracticeSessionDetailRepository practiceSessionDetailRepository;
    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private FlaggedQuestionRepository flaggedQuestionRepository;

    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    public Optional<Question> getQuestionById(Long id) {
        return questionRepository.findById(id);
    }

    public List<Question> getQuestionsByChapter(String chapterId) {
        return questionRepository.findByChapter_ChapterId(chapterId);
    }

    public List<Question> getQuestionsBySubject(String subjectId) {
        return questionRepository.findBySubjectId(subjectId);
    }

    public Question saveQuestion(Question question) {
        normalizeCorrectAnswers(question);
        return questionRepository.save(question);
    }

    public List<Question> saveQuestions(List<Question> questions) {
        questions.forEach(this::normalizeCorrectAnswers);
        return questionRepository.saveAll(questions);
    }

    public List<Question> saveQuestionsFromExcel(MultipartFile file) {
        List<Question> questions = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
                Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheet("Questions");

            for (Row row : sheet) {
                if (row.getRowNum() == 0)
                    continue; // Skip header row

                Question question = new Question();
                // QuestionId is auto-generated, so we don't set it from Excel
                String chapterId = getCellValue(row.getCell(0));

                // Skip empty rows
                if (chapterId == null || chapterId.trim().isEmpty()) {
                    continue;
                }

                question.setQuestionText(getCellValue(row.getCell(1)));
                question.setCorrectAnswer(getCellValue(row.getCell(2)));
                question.setOptionA(getCellValue(row.getCell(3)));
                question.setOptionB(getCellValue(row.getCell(4)));
                question.setOptionC(getCellValue(row.getCell(5)));
                question.setOptionD(getCellValue(row.getCell(6)));
                question.setOptionE(getCellValue(row.getCell(7)));
                question.setOptionF(getCellValue(row.getCell(8)));
                question.setExplanation(getCellValue(row.getCell(9)));
                question.setDifficultyLevel(getCellValue(row.getCell(10)));

                // Handle QuestionType - default to 1 if empty
                String questionTypeStr = getCellValue(row.getCell(11));
                question.setQuestionType(questionTypeStr != null && !questionTypeStr.trim().isEmpty()
                        ? Integer.parseInt(questionTypeStr.trim())
                        : 1);

                question.setCorrectAnswers(getCellValue(row.getCell(12)));
                question.setNotes(getCellValue(row.getCell(13)));

                Chapter chapter = chapterRepository.findById(chapterId)
                        .orElseThrow(() -> new RuntimeException("Chapter not found for ID: " + chapterId));

                question.setChapter(chapter);

                questions.add(question);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage());
        }

        return questionRepository.saveAll(questions);
    }

    // Helper method to get cell values as strings
    private String getCellValue(Cell cell) {
        if (cell == null)
            return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    public void deleteQuestion(Long id) {
        questionRepository.deleteById(id);
    }

    // Unpracticed questions by subject
    public List<Question> getUnpracticedQuestionsBySubject(Long studentId, String subjectId, int limit) {
        // Get practiced question IDs for the student
        List<Long> practicedQuestionIds = practiceSessionDetailRepository.findAnsweredQuestionIds(studentId);

        // If no questions have been practiced, return questions with limit
        if (practicedQuestionIds == null || practicedQuestionIds.isEmpty()) {
            return questionRepository.findBySubjectIdWithLimit(subjectId, limit);
        }

        // Get unpracticed questions for the subject
        return questionRepository.findUnpracticedQuestionsBySubject(subjectId, practicedQuestionIds, limit);
    }

    // Unpracticed questions by chapter
    public List<Question> getUnpracticedQuestionsByChapter(Long studentId, String chapterId, int limit, String level) {
        // Normalize level parameter - treat "all" as null to get all difficulty levels
        String normalizedLevel = (level != null && level.equalsIgnoreCase("all")) ? null : level;

        // Get answered question IDs for the student and chapter
        List<Long> practicedQuestionIds = practiceSessionDetailRepository
                .findCorrectlyAnsweredQuestionIdsByChapter(studentId, chapterId);

        if (practicedQuestionIds == null || practicedQuestionIds.isEmpty()) {
            return questionRepository.findByChapterIdWithLimit(chapterId, limit);
        }
        // Get unpracticed questions for the chapter
        return questionRepository.findUnpracticedQuestionsByChapter(chapterId, practicedQuestionIds, limit,
                normalizedLevel);
    }

    // Get unpracticed questions by chapter with flag status
    public List<QuestionWithFlagDTO> getUnpracticedQuestionsByChapterWithFlags(
            Long studentId, String chapterId, int limit, String level) {
        List<Question> questions = getUnpracticedQuestionsByChapter(studentId, chapterId, limit, level);
        return addFlagStatusToQuestions(questions, studentId);
    }

    // Get unpracticed questions by subject with flag status
    public List<QuestionWithFlagDTO> getUnpracticedQuestionsBySubjectWithFlags(
            Long studentId, String subjectId, int limit) {
        List<Question> questions = getUnpracticedQuestionsBySubject(studentId, subjectId, limit);
        return addFlagStatusToQuestions(questions, studentId);
    }

    // Get questions by chapter with flag status
    public List<QuestionWithFlagDTO> getQuestionsByChapterWithFlags(String chapterId, Long studentId) {
        List<Question> questions = getQuestionsByChapter(chapterId);
        return addFlagStatusToQuestions(questions, studentId);
    }

    // Get questions by subject with flag status
    public List<QuestionWithFlagDTO> getQuestionsBySubjectWithFlags(String subjectId, Long studentId) {
        List<Question> questions = getQuestionsBySubject(subjectId);
        return addFlagStatusToQuestions(questions, studentId);
    }

    /**
     * Get questions that were answered incorrectly and haven't been correctly
     * answered yet (by chapter)
     * These are questions the student got wrong and still need to practice
     */
    public List<QuestionWithFlagDTO> getWrongUnpracticedQuestionsByChapter(Long studentId, String chapterId) {
        // Get all incorrectly answered question IDs for this chapter
        List<Long> incorrectQuestionIds = practiceSessionDetailRepository
                .findIncorrectlyAnsweredQuestionIdsByChapter(studentId, chapterId);

        if (incorrectQuestionIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Get all correctly answered question IDs for this chapter
        Set<Long> correctQuestionIds = practiceSessionDetailRepository
                .findCorrectlyAnsweredQuestionIdsByChapter(studentId, chapterId)
                .stream()
                .collect(Collectors.toSet());

        // Filter out questions that have been correctly answered
        List<Long> wrongUnpracticedIds = incorrectQuestionIds.stream()
                .filter(id -> !correctQuestionIds.contains(id))
                .distinct()
                .collect(Collectors.toList());

        if (wrongUnpracticedIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Fetch the questions
        List<Question> questions = questionRepository.findAllById(wrongUnpracticedIds);

        // Add flag status and return
        return addFlagStatusToQuestions(questions, studentId);
    }

    /**
     * Get questions that were answered incorrectly and haven't been correctly
     * answered yet (by subject)
     * These are questions the student got wrong and still need to practice
     */
    public List<QuestionWithFlagDTO> getWrongUnpracticedQuestionsBySubject(Long studentId, String subjectId) {
        // Get all incorrectly answered question IDs for this subject
        List<Long> incorrectQuestionIds = practiceSessionDetailRepository
                .findIncorrectlyAnsweredQuestionIdsBySubject(studentId, subjectId);

        if (incorrectQuestionIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Get all correctly answered question IDs for this student (across all
        // subjects)
        Set<Long> correctQuestionIds = practiceSessionDetailRepository
                .findAnsweredQuestionIds(studentId)
                .stream()
                .filter(id -> {
                    // Check if this question was answered correctly
                    return practiceSessionDetailRepository
                            .findByStudentIdAndQuestionId(studentId, id)
                            .map(psd -> psd.getIsCorrect())
                            .orElse(false);
                })
                .collect(Collectors.toSet());

        // Filter out questions that have been correctly answered
        List<Long> wrongUnpracticedIds = incorrectQuestionIds.stream()
                .filter(id -> !correctQuestionIds.contains(id))
                .distinct()
                .collect(Collectors.toList());

        if (wrongUnpracticedIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Fetch the questions
        List<Question> questions = questionRepository.findAllById(wrongUnpracticedIds);

        // Add flag status and return
        return addFlagStatusToQuestions(questions, studentId);
    }

    // Helper method to add flag status to questions
    private List<QuestionWithFlagDTO> addFlagStatusToQuestions(List<Question> questions, Long studentId) {
        // Get all flagged question IDs for this student
        Set<Long> flaggedQuestionIds = flaggedQuestionRepository
                .findByStudent_StudentId(studentId)
                .stream()
                .map(fq -> fq.getQuestion().getQuestionId())
                .collect(Collectors.toSet());

        // Convert questions to DTOs with flag status
        return questions.stream()
                .map(q -> new QuestionWithFlagDTO(q, flaggedQuestionIds.contains(q.getQuestionId())))
                .collect(Collectors.toList());
    }

    private void normalizeCorrectAnswers(Question question) {
        String correctAnswer = question.getCorrectAnswer();
        String correctAnswers = question.getCorrectAnswers();

        if (correctAnswer != null) {
            correctAnswer = correctAnswer.trim();
            question.setCorrectAnswer(correctAnswer.isEmpty() ? null : correctAnswer);
        }

        if (correctAnswers != null) {
            correctAnswers = correctAnswers.trim();
            question.setCorrectAnswers(correctAnswers.isEmpty() ? null : correctAnswers);
        }

        if (question.getQuestionType() == 4) {
            if ((question.getCorrectAnswers() == null || question.getCorrectAnswers().isBlank())
                    && question.getCorrectAnswer() != null && !question.getCorrectAnswer().isBlank()) {
                question.setCorrectAnswers(question.getCorrectAnswer());
            }
            if (question.getCorrectAnswer() == null) {
                question.setCorrectAnswer("");
            }
        } else {
            question.setCorrectAnswers(question.getCorrectAnswers());
        }
    }

    // public List<Question> getQuestionsByChapter(Long chapterId) {
    // return questionRepository.findQuestionsByChapterExcludingPractice(chapterId);
    // }
}
