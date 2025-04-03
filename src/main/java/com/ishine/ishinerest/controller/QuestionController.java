package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.entity.Question;
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
            question.setCorrectAnswer(questionDetails.getCorrectAnswer());
            question.setQuestionType(questionDetails.getQuestionType());

            return questionService.saveQuestion(question);
        }
        return null;
    }

    @DeleteMapping("/{id}")
    public void deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
    }

    // Endpoint for unpracticed questions by subject
    @GetMapping("/unpracticed/subject")
    public List<Question> getUnpracticedQuestionsBySubject(@RequestParam Long studentId, @RequestParam String subjectId) {
        return questionService.getUnpracticedQuestionsBySubject(studentId, subjectId);
    }

    // Endpoint for unpracticed questions by chapter
    @GetMapping("/unpracticed/chapter")
    public List<Question> getUnpracticedQuestionsByChapter(
            @RequestParam Long studentId,
            @RequestParam String chapterId,
            @RequestParam(defaultValue = "10") int limit) {
        return questionService.getUnpracticedQuestionsByChapter(studentId, chapterId, limit);
    }
}
