package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.entity.ExamQuestion;
import com.ishine.ishinerest.service.ExamQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/examquestions")
public class ExamQuestionController {

    @Autowired
    private ExamQuestionService examQuestionService;

    // Get all ExamQuestions
    @GetMapping
    public List<ExamQuestion> getAllExamQuestions() {
        return examQuestionService.getAllExamQuestions();
    }

    // Get an ExamQuestion by ID
    @GetMapping("/{id}")
    public ResponseEntity<ExamQuestion> getExamQuestionById(@PathVariable Long id) {
        Optional<ExamQuestion> examQuestion = examQuestionService.getExamQuestionById(id);
        return examQuestion.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Create a new ExamQuestion
    @PostMapping
    public List<ExamQuestion> createExamQuestion(@RequestBody List<ExamQuestion> examQuestions) {
        return examQuestionService.saveExamQuestions(examQuestions);
    }

    // Update an existing ExamQuestion
    @PutMapping("/{id}")
    public ResponseEntity<ExamQuestion> updateExamQuestion(
            @PathVariable Long id, @RequestBody ExamQuestion examQuestionDetails) {
        Optional<ExamQuestion> examQuestion = examQuestionService.getExamQuestionById(id);
        if (examQuestion.isPresent()) {
            ExamQuestion existingExamQuestion = examQuestion.get();
            existingExamQuestion.setExamPaper(examQuestionDetails.getExamPaper());
            existingExamQuestion.setQuestionText(examQuestionDetails.getQuestionText());
            existingExamQuestion.setQuestionType(examQuestionDetails.getQuestionType());
            existingExamQuestion.setSolution(examQuestionDetails.getSolution());
            existingExamQuestion.setQuestionImage(examQuestionDetails.getQuestionImage());
            existingExamQuestion.setMarkingSchemeImage(examQuestionDetails.getMarkingSchemeImage());

            ExamQuestion updatedExamQuestion = examQuestionService.saveExamQuestion(existingExamQuestion);
            return ResponseEntity.ok(updatedExamQuestion);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Delete an ExamQuestion
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExamQuestion(@PathVariable Long id) {
        Optional<ExamQuestion> examQuestion = examQuestionService.getExamQuestionById(id);
        if (examQuestion.isPresent()) {
            examQuestionService.deleteExamQuestionById(id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/chapter/{chapterId}")
    public List<ExamQuestion> getExamQuestionsByChapter(@PathVariable String chapterId) {
        return examQuestionService.getExamQuestionsByChapter(chapterId);
    }
}
