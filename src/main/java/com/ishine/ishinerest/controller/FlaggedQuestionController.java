package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.entity.FlaggedQuestion;
import com.ishine.ishinerest.pojo.QuestionWithFlagDTO;
import com.ishine.ishinerest.service.FlaggedQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/flagged-questions")
public class FlaggedQuestionController {

    @Autowired
    private FlaggedQuestionService flaggedQuestionService;

    /**
     * Flag a question for a student
     * POST /flagged-questions/student/{studentId}/question/{questionId}
     */
    @PostMapping("/student/{studentId}/question/{questionId}")
    public ResponseEntity<FlaggedQuestion> flagQuestion(
            @PathVariable Long studentId,
            @PathVariable Long questionId) {
        FlaggedQuestion flaggedQuestion = flaggedQuestionService.flagQuestion(studentId, questionId);
        return ResponseEntity.status(HttpStatus.CREATED).body(flaggedQuestion);
    }

    /**
     * Unflag a question for a student
     * DELETE /flagged-questions/student/{studentId}/question/{questionId}
     */
    @DeleteMapping("/student/{studentId}/question/{questionId}")
    public ResponseEntity<Void> unflagQuestion(
            @PathVariable Long studentId,
            @PathVariable Long questionId) {
        flaggedQuestionService.unflagQuestion(studentId, questionId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get all flagged questions for a student in a specific chapter (NOW WITH
     * isFlagged)
     * GET /flagged-questions/student/{studentId}/chapter/{chapterId}
     */
    @GetMapping("/student/{studentId}/chapter/{chapterId}")
    public ResponseEntity<List<QuestionWithFlagDTO>> getFlaggedQuestionsByChapter(
            @PathVariable Long studentId,
            @PathVariable String chapterId) {
        List<QuestionWithFlagDTO> flaggedQuestions = flaggedQuestionService.getFlaggedQuestionsByChapterWithFlags(
                studentId,
                chapterId);
        return ResponseEntity.ok(flaggedQuestions);
    }

    /**
     * Get all flagged questions for a student (across all chapters) (NOW WITH
     * isFlagged)
     * GET /flagged-questions/student/{studentId}
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<QuestionWithFlagDTO>> getAllFlaggedQuestions(@PathVariable Long studentId) {
        List<QuestionWithFlagDTO> flaggedQuestions = flaggedQuestionService.getAllFlaggedQuestionsWithFlags(studentId);
        return ResponseEntity.ok(flaggedQuestions);
    }

    /**
     * Get unpracticed flagged questions for a student in a specific chapter
     * (excludes correctly answered flagged questions)
     * GET /flagged-questions/student/{studentId}/chapter/{chapterId}/unpracticed
     */
    @GetMapping("/student/{studentId}/chapter/{chapterId}/unpracticed")
    public ResponseEntity<List<QuestionWithFlagDTO>> getUnpracticedFlaggedQuestionsByChapter(
            @PathVariable Long studentId,
            @PathVariable String chapterId) {
        List<QuestionWithFlagDTO> unpracticedFlaggedQuestions = flaggedQuestionService
                .getUnpracticedFlaggedQuestionsByChapter(studentId, chapterId);
        return ResponseEntity.ok(unpracticedFlaggedQuestions);
    }
}

// Made with Bob
