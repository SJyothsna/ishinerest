package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.pojo.PrevExamQuestionResponse;
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

    @GetMapping("/by-chapter-level")
    public ResponseEntity<List<PrevExamQuestionResponse>> getQuestionsByChapterAndLevel(
            @RequestParam String chapterId,
            @RequestParam String level) {

        List<PrevExamQuestionResponse> questions = prevExamQuestionService.getQuestionsByChapterAndLevel(chapterId, level);
        return ResponseEntity.ok(questions);
    }
}
