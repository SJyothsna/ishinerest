package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.entity.PrevExamPaper;
import com.ishine.ishinerest.pojo.PrevExamPaperResponse;
import com.ishine.ishinerest.service.PrevExamPaperService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/prevexam-papers")
public class PrevExamPaperController {

    private final PrevExamPaperService paperService;

    public PrevExamPaperController(PrevExamPaperService paperService) {
        this.paperService = paperService;
    }

    // GET endpoint to fetch all papers
    @GetMapping
    public ResponseEntity<List<PrevExamPaper>> getAllPapers() {
        List<PrevExamPaper> papers = paperService.getAllPapers();
        return ResponseEntity.ok(papers);
    }
    @GetMapping("/by-subject")
    public ResponseEntity<List<PrevExamPaperResponse>> getPaperDetailsBySubjectId(@RequestParam String subjectId) {
        return ResponseEntity.ok(paperService.getPaperDetailsBySubjectId(subjectId));
    }

    // POST endpoint to upload paper details
    @PostMapping
    public ResponseEntity<PrevExamPaper> uploadPaper(@RequestBody PrevExamPaper paper) {
        PrevExamPaper savedPaper = paperService.savePaper(paper);
        return ResponseEntity.ok(savedPaper);
    }
    @PostMapping("/upload")
    public ResponseEntity<String> uploadExcel(@RequestParam("file") MultipartFile file) {
        try {
            paperService.processExcelUpload(file);
            return ResponseEntity.ok("Excel uploaded and processed successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to process Excel: " + e.getMessage());
        }
    }

}
