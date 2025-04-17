package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.Chapter;
import com.ishine.ishinerest.entity.Question;
import com.ishine.ishinerest.repository.ChapterRepository;
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

@Service
public class QuestionService {

    @Autowired
    private PracticeSessionDetailRepository practiceSessionDetailRepository;
    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private QuestionRepository questionRepository;

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
        return questionRepository.save(question);
    }

    public List<Question> saveQuestions(List<Question> questions) {
        return questionRepository.saveAll(questions);
    }

    public List<Question> saveQuestionsFromExcel(MultipartFile file) {
        List<Question> questions = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheet("Questions");

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;  // Skip header row

                Question question = new Question();
                question.setQuestionId(Long.parseLong(getCellValue(row.getCell(0))));
                question.setQuestionText(getCellValue(row.getCell(2)));
                question.setOptionA(getCellValue(row.getCell(4)));
                question.setOptionB(getCellValue(row.getCell(5)));
                question.setOptionC(getCellValue(row.getCell(6)));
                question.setOptionD(getCellValue(row.getCell(7)));
                question.setCorrectAnswer(getCellValue(row.getCell(3)));
                question.setQuestionType(Integer.parseInt(getCellValue(row.getCell(11))));
                question.setExplanation(getCellValue(row.getCell(8)));
                question.setImagePath(getCellValue(row.getCell(9)));
                question.setDifficultyLevel(getCellValue(row.getCell(10)));
                String chapterId = getCellValue(row.getCell(1));
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
        if (cell == null) return "";

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
    public List<Question> getUnpracticedQuestionsBySubject(Long studentId, String subjectId) {
        // Get practiced question IDs for the student
        List<Long> practicedQuestionIds = practiceSessionDetailRepository.findIncorrectlyAnsweredQuestionIds(studentId);

        // Get unpracticed questions for the subject
        return questionRepository.findUnpracticedQuestionsBySubject(subjectId, practicedQuestionIds);
    }

    // Unpracticed questions by chapter
    public List<Question> getUnpracticedQuestionsByChapter(Long studentId, String chapterId, int limit) {
        // Get answered question IDs for the student and chapter
        List<Long> practicedQuestionIds = practiceSessionDetailRepository.findCorrectlyAnsweredQuestionIdsByChapter(studentId, chapterId);

        if (practicedQuestionIds == null || practicedQuestionIds.isEmpty()) {
            return questionRepository.findByChapterIdWithLimit(chapterId, limit);
        }
        // Get unpracticed questions for the chapter
        return questionRepository.findUnpracticedQuestionsByChapter(chapterId, practicedQuestionIds, limit);
    }

//    public List<Question> getQuestionsByChapter(Long chapterId) {
//        return questionRepository.findQuestionsByChapterExcludingPractice(chapterId);
//    }
}
