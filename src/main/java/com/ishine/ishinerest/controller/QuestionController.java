package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.firebase.QuestionService;
import com.ishine.ishinerest.pojo.Question;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
//hello jyo how r u
@RestController
@RequestMapping("/questions")
public class QuestionController {
    @Autowired
    private QuestionService questionService;
    @GetMapping()
    public List<Question> getQuestions() throws ExecutionException, InterruptedException {
        List<Question> questions = questionService.getAllQuestions();
        return questions;
    }

    @PostMapping()
    public void addQuestions(@RequestBody List<Question> questions) throws ExecutionException, InterruptedException {
        questionService.addQuestions(questions);
    }

    @GetMapping("/topic/{topicId}")
    public List<Question> getQuestionsByTopicId(@PathVariable long topicId) {
        return questionService.getQuestionsByTopic(topicId);
    }

    @GetMapping("/subject/{subjectId}")
    public List<Question> getQuestionsBySubjectId(@PathVariable long subjectId) {
        return questionService.getQuestionsBySubject(subjectId);
    }
}
