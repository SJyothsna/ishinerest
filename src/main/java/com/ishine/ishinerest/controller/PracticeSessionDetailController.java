package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.entity.PracticeSessionDetail;
import com.ishine.ishinerest.pojo.QuestionWithFlagDTO;
import com.ishine.ishinerest.service.PracticeSessionDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/practice-session-details")
public class PracticeSessionDetailController {

    @Autowired
    private PracticeSessionDetailService service;

    @GetMapping
    public List<PracticeSessionDetail> getAllSessionDetails() {
        return service.getAllSessionDetails();
    }

    @GetMapping("/{id}")
    public List<PracticeSessionDetail> getSessionDetailByStudentId(@PathVariable Long id) {
        List<PracticeSessionDetail> sessionDetails = service.getSessionDetailByStudentId(id);
        return sessionDetails;
    }

    @PostMapping("/{studentId}")
    public List<PracticeSessionDetail> createSessionDetail(
            @PathVariable Long studentId,
            @RequestBody List<PracticeSessionDetail> sessionDetails) {
        return service.saveSessionDetails(studentId, sessionDetails);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSessionDetail(@PathVariable Long id) {
        service.deleteSessionDetail(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/reset/student/{studentId}/chapter/{chapterId}")
    public ResponseEntity<String> resetChapterProgress(
            @PathVariable Long studentId,
            @PathVariable String chapterId) {
        service.resetChapterProgress(studentId, chapterId);
        return ResponseEntity.ok("Progress reset successfully for student " + studentId + " in chapter " + chapterId);
    }

    /**
     * Get wrong answers (incorrectly answered questions) for a student in a chapter
     * WITH isFlagged
     * GET
     * /practice-session-details/wrong-answers/student/{studentId}/chapter/{chapterId}
     */
    @GetMapping("/wrong-answers/student/{studentId}/chapter/{chapterId}")
    public ResponseEntity<List<QuestionWithFlagDTO>> getWrongAnswersByChapter(
            @PathVariable Long studentId,
            @PathVariable String chapterId) {
        List<QuestionWithFlagDTO> wrongAnswers = service.getWrongAnswersByChapterWithFlags(studentId, chapterId);
        return ResponseEntity.ok(wrongAnswers);
    }

}