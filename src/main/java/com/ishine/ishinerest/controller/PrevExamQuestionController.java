package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.entity.PrevExamPaper;
import com.ishine.ishinerest.pojo.PrevExamQuestionResponse;
import org.springframework.web.multipart.MultipartFile;
import com.ishine.ishinerest.service.PrevExamQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/prevexam-questions")
public class PrevExamQuestionController {

    @Autowired
    private PrevExamQuestionService prevExamQuestionService;


    @GetMapping("/by-chapter")
    public List<PrevExamQuestionResponse> getQuestionsByChapter(@RequestParam String chapterId) {
        return prevExamQuestionService.getQuestionsByChapterId(chapterId);
    }

    @GetMapping("/by-subject")
    public List<PrevExamQuestionResponse> getQuestionsBySubject(@RequestParam String subjectId) {
        return prevExamQuestionService.getPaperDetailsBySubjectId(subjectId);
    }
    @GetMapping("/all")
    public List<PrevExamPaper> getAllExamPapers() {
        return prevExamQuestionService.getAllPrevExamPapers();
    }
    @PostMapping("/upload")
    public ResponseEntity<String> uploadQuestions(@RequestParam("file") MultipartFile file) {
        try {
            prevExamQuestionService.saveQuestionsFromExcel(file);
            return ResponseEntity.ok("File uploaded and questions saved successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error while processing file: " + e.getMessage());
        }
    }
}
