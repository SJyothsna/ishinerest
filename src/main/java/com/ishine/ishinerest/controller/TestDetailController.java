package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.entity.TestDetail;
import com.ishine.ishinerest.pojo.QuestionWithFlagDTO;
import com.ishine.ishinerest.service.TestDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/test-details")
@RequiredArgsConstructor
public class TestDetailController {

    private final TestDetailService service;

    @GetMapping
    public List<TestDetail> getAllTestDetails() {
        return service.getAllTestDetails();
    }

    @GetMapping("/{studentId}")
    public List<TestDetail> getTestDetailsByStudentId(@PathVariable Long studentId) {
        return service.getTestDetailsByStudentId(studentId);
    }

    @GetMapping("/student/{studentId}/assignment/{assignmentId}")
    public List<TestDetail> getTestDetailsByStudentAndAssignmentId(
            @PathVariable Long studentId,
            @PathVariable Long assignmentId) {
        return service.getTestDetailsByStudentAndAssignmentId(studentId, assignmentId);
    }

    @PostMapping("/{studentId}")
    public List<TestDetail> createTestDetails(
            @PathVariable Long studentId,
            @RequestBody List<TestDetail> testDetails) {
        return service.saveTestDetails(studentId, testDetails);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTestDetail(@PathVariable Long id) {
        service.deleteTestDetail(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/reset/student/{studentId}")
    public ResponseEntity<String> resetStudentProgress(@PathVariable Long studentId) {
        service.resetStudentProgress(studentId);
        return ResponseEntity.ok("Progress reset successfully for student " + studentId);
    }

    @DeleteMapping("/reset/student/{studentId}/assignment/{assignmentId}")
    public ResponseEntity<String> resetAssignmentProgress(
            @PathVariable Long studentId,
            @PathVariable Long assignmentId) {
        service.resetAssignmentProgress(studentId, assignmentId);
        return ResponseEntity.ok("Progress reset successfully for student " + studentId + " in assignment " + assignmentId);
    }

    @GetMapping("/wrong-answers/student/{studentId}/assignment/{assignmentId}")
    public ResponseEntity<List<QuestionWithFlagDTO>> getWrongAnswersByAssignment(
            @PathVariable Long studentId,
            @PathVariable Long assignmentId) {
        List<QuestionWithFlagDTO> wrongAnswers = service.getWrongAnswersByAssignmentWithFlags(studentId, assignmentId);
        return ResponseEntity.ok(wrongAnswers);
    }
}

 // Made with Bob
