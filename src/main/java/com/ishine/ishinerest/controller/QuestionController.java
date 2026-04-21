package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.entity.Question;
import com.ishine.ishinerest.pojo.QuestionWithFlagDTO;
import com.ishine.ishinerest.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/questions")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @GetMapping
    public List<Question> getAllQuestions() {
        return questionService.getAllQuestions();
    }

    @GetMapping("/{id}")
    public Optional<Question> getQuestionById(@PathVariable Long id) {
        return questionService.getQuestionById(id);
    }

    @GetMapping("/chapter/{chapterId}")
    public List<Question> getQuestionsByChapter(@PathVariable String chapterId) {
        return questionService.getQuestionsByChapter(chapterId);
    }

    @GetMapping("/subject/{subjectId}")
    public List<Question> getQuestionsBySubject(@PathVariable String subjectId) {
        return questionService.getQuestionsBySubject(subjectId);
    }

    @PostMapping
    public List<Question> createQuestions(@RequestBody List<Question> questions) {
        return questionService.saveQuestions(questions);
    }

    @PostMapping("/upload")
    public List<Question> uploadQuestionsFromExcel(@RequestParam("file") MultipartFile file) {
        return questionService.saveQuestionsFromExcel(file);
    }

    @PutMapping("/{id}")
    public Question updateQuestions(@PathVariable Long id, @RequestBody Question questionDetails) {
        Optional<Question> questionOptional = questionService.getQuestionById(id);
        if (questionOptional.isPresent()) {
            Question question = questionOptional.get();
            question.setQuestionText(questionDetails.getQuestionText());
            question.setOptionA(questionDetails.getOptionA());
            question.setOptionB(questionDetails.getOptionB());
            question.setOptionC(questionDetails.getOptionC());
            question.setOptionD(questionDetails.getOptionD());
            question.setOptionE(questionDetails.getOptionE());
            question.setOptionF(questionDetails.getOptionF());
            question.setCorrectAnswer(questionDetails.getCorrectAnswer());
            question.setCorrectAnswers(questionDetails.getCorrectAnswers());
            question.setQuestionType(questionDetails.getQuestionType());
            question.setDifficultyLevel(questionDetails.getDifficultyLevel());
            question.setExplanation(questionDetails.getExplanation());
            question.setNotes(questionDetails.getNotes());

            return questionService.saveQuestion(question);
        }
        return null;
    }

    @DeleteMapping("/{id}")
    public void deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
    }

    // Endpoint for unpracticed questions by subject (NOW WITH isFlagged)
    @GetMapping("/unpracticed/subject")
    public List<QuestionWithFlagDTO> getUnpracticedQuestionsBySubject(
            @RequestParam Long studentId,
            @RequestParam String subjectId,
            @RequestParam(defaultValue = "10") int limit) {
        return questionService.getUnpracticedQuestionsBySubjectWithFlags(studentId, subjectId, limit);
    }

    // Endpoint for unpracticed questions by chapter (NOW WITH isFlagged)
    @GetMapping("/unpracticed/chapter")
    public List<QuestionWithFlagDTO> getUnpracticedQuestionsByChapter(
            @RequestParam Long studentId,
            @RequestParam String chapterId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(name = "level", required = false) String level) {
        return questionService.getUnpracticedQuestionsByChapterWithFlags(studentId, chapterId, limit, level);
    }

    // NEW ENDPOINTS WITH FLAG STATUS

    // Endpoint for unpracticed questions by chapter with flag status
    @GetMapping("/unpracticed/chapter/with-flags")
    public List<QuestionWithFlagDTO> getUnpracticedQuestionsByChapterWithFlags(
            @RequestParam Long studentId,
            @RequestParam String chapterId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(name = "level", required = false) String level) {
        return questionService.getUnpracticedQuestionsByChapterWithFlags(studentId, chapterId, limit, level);
    }

    // Endpoint for unpracticed questions by subject with flag status
    @GetMapping("/unpracticed/subject/with-flags")
    public List<QuestionWithFlagDTO> getUnpracticedQuestionsBySubjectWithFlags(
            @RequestParam Long studentId,
            @RequestParam String subjectId,
            @RequestParam(defaultValue = "10") int limit) {
        return questionService.getUnpracticedQuestionsBySubjectWithFlags(studentId, subjectId, limit);
    }

    // Endpoint for questions by chapter with flag status
    @GetMapping("/chapter/{chapterId}/with-flags")
    public List<QuestionWithFlagDTO> getQuestionsByChapterWithFlags(
            @PathVariable String chapterId,
            @RequestParam Long studentId) {
        return questionService.getQuestionsByChapterWithFlags(chapterId, studentId);
    }

    // Endpoint for questions by subject with flag status
    @GetMapping("/subject/{subjectId}/with-flags")
    public List<QuestionWithFlagDTO> getQuestionsBySubjectWithFlags(
            @PathVariable String subjectId,
            @RequestParam Long studentId) {
        return questionService.getQuestionsBySubjectWithFlags(subjectId, studentId);
    }

    /**
     * Get questions that were answered incorrectly and haven't been correctly
     * answered yet (by chapter)
     * These are questions the student got wrong and still need to practice
     * GET /questions/wrong-unpracticed/chapter?studentId=X&chapterId=Y
     */
    @GetMapping("/wrong-unpracticed/chapter")
    public List<QuestionWithFlagDTO> getWrongUnpracticedQuestionsByChapter(
            @RequestParam Long studentId,
            @RequestParam String chapterId) {
        return questionService.getWrongUnpracticedQuestionsByChapter(studentId, chapterId);
    }

    /**
     * Get questions that were answered incorrectly and haven't been correctly
     * answered yet (by subject)
     * These are questions the student got wrong and still need to practice
     * GET /questions/wrong-unpracticed/subject?studentId=X&subjectId=Y
     */
    @GetMapping("/wrong-unpracticed/subject")
    public List<QuestionWithFlagDTO> getWrongUnpracticedQuestionsBySubject(
            @RequestParam Long studentId,
            @RequestParam String subjectId) {
        return questionService.getWrongUnpracticedQuestionsBySubject(studentId, subjectId);
    }
}
