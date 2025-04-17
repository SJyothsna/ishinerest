package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.entity.PrevExamPaper;
import com.ishine.ishinerest.pojo.PrevExamQuestionResponse;
import com.ishine.ishinerest.repository.PrevExamPaperRepository;
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
    @GetMapping("/all")
    public List<PrevExamPaper> getAllExamPapers() {
        return prevExamQuestionService.getAllPrevExamPapers();
    }

}
