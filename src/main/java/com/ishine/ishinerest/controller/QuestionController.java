package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.entity.Question;
import com.ishine.ishinerest.pojo.QuestionWithFlagDTO;
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
    public List<Question> getAllQuestions(
            @RequestParam(name = "createdBy", required = false) Long createdBy) {
        if (createdBy != null) {
            return questionService.getQuestionsByCreatorOrDefault(createdBy);
        }
        return questionService.getQuestionsByCreatorOrDefault(null);
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
            question.setSectionId(questionDetails.getSectionId());
            question.setQuestionText(questionDetails.getQuestionText());
            question.setOptionA(questionDetails.getOptionA());
            question.setOptionB(questionDetails.getOptionB());
            question.setOptionC(questionDetails.getOptionC());
            question.setOptionD(questionDetails.getOptionD());
            question.setOptionE(questionDetails.getOptionE());
            question.setOptionF(questionDetails.getOptionF());
            question.setCorrectAnswer(questionDetails.getCorrectAnswer());
            question.setCorrectAnswers(questionDetails.getCorrectAnswers());
            question.setQuestionType(questionDetails.getQuestionType());
            question.setDifficultyLevel(questionDetails.getDifficultyLevel());
            question.setExplanation(questionDetails.getExplanation());
            question.setNotes(questionDetails.getNotes());
            question.setHint(questionDetails.getHint());
            question.setUsageType(questionDetails.getUsageType());
            question.setQuestionImageUrl(questionDetails.getQuestionImageUrl());

            return questionService.saveQuestion(question);
        }
        return null;
    }

    @DeleteMapping("/{id}")
    public void deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
    }

    // Endpoint for unpracticed questions by subject (NOW WITH isFlagged)
    @GetMapping("/unpracticed/subject")
    public List<QuestionWithFlagDTO> getUnpracticedQuestionsBySubject(
            @RequestParam Long studentId,
            @RequestParam String subjectId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(name = "usageType", required = false) String usageType) {
        return questionService.getUnpracticedQuestionsBySubjectWithFlags(studentId, subjectId, limit, usageType);
    }

    // Endpoint for unpracticed questions by chapter (NOW WITH isFlagged)
    @GetMapping("/unpracticed/chapter")
    public List<QuestionWithFlagDTO> getUnpracticedQuestionsByChapter(
            @RequestParam Long studentId,
            @RequestParam String chapterId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "usageType", required = false) String usageType,
            @RequestParam(name = "sectionId", required = false) String sectionId) {
        return questionService.getUnpracticedQuestionsByChapterWithFlags(studentId, chapterId, limit, level, usageType, sectionId);
    }

    // NEW ENDPOINTS WITH FLAG STATUS

    // Endpoint for unpracticed questions by chapter with flag status
    @GetMapping("/unpracticed/chapter/with-flags")
    public List<QuestionWithFlagDTO> getUnpracticedQuestionsByChapterWithFlags(
            @RequestParam Long studentId,
            @RequestParam String chapterId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(name = "level", required = false) String level,
            @RequestParam(name = "usageType", required = false) String usageType,
            @RequestParam(name = "sectionId", required = false) String sectionId) {
        return questionService.getUnpracticedQuestionsByChapterWithFlags(studentId, chapterId, limit, level, usageType, sectionId);
    }

    // Endpoint for unpracticed questions by subject with flag status
    @GetMapping("/unpracticed/subject/with-flags")
    public List<QuestionWithFlagDTO> getUnpracticedQuestionsBySubjectWithFlags(
            @RequestParam Long studentId,
            @RequestParam String subjectId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(name = "usageType", required = false) String usageType) {
        return questionService.getUnpracticedQuestionsBySubjectWithFlags(studentId, subjectId, limit, usageType);
    }

    // Endpoint for questions by chapter with flag status
    @GetMapping("/chapter/{chapterId}/with-flags")
    public List<QuestionWithFlagDTO> getQuestionsByChapterWithFlags(
            @PathVariable String chapterId,
            @RequestParam Long studentId) {
        return questionService.getQuestionsByChapterWithFlags(chapterId, studentId);
    }

    // Endpoint for questions by subject with flag status
    @GetMapping("/subject/{subjectId}/with-flags")
    public List<QuestionWithFlagDTO> getQuestionsBySubjectWithFlags(
            @PathVariable String subjectId,
            @RequestParam Long studentId) {
        return questionService.getQuestionsBySubjectWithFlags(subjectId, studentId);
    }

    /**
     * Get questions that were answered incorrectly and haven't been correctly
     * answered yet (by chapter)
     * These are questions the student got wrong and still need to practice
     * GET /questions/wrong-unpracticed/chapter?studentId=X&chapterId=Y
     */
    @GetMapping("/wrong-unpracticed/chapter")
    public List<QuestionWithFlagDTO> getWrongUnpracticedQuestionsByChapter(
            @RequestParam Long studentId,
            @RequestParam String chapterId) {
        return questionService.getWrongUnpracticedQuestionsByChapter(studentId, chapterId);
    }

    /**
     * Get questions that were answered incorrectly and haven't been correctly
     * answered yet (by subject)
     * These are questions the student got wrong and still need to practice
     * GET /questions/wrong-unpracticed/subject?studentId=X&subjectId=Y
     */
    @GetMapping("/wrong-unpracticed/subject")
    public List<QuestionWithFlagDTO> getWrongUnpracticedQuestionsBySubject(
            @RequestParam Long studentId,
            @RequestParam String subjectId) {
        return questionService.getWrongUnpracticedQuestionsBySubject(studentId, subjectId);
    }
    
    // ========================================================================
    // CUSTOM QUESTION ENDPOINTS (for parents/teachers)
    // ========================================================================
    
    /**
     * Create a custom question
     * POST /questions/custom?creatorUserId=X
     */
    @PostMapping("/custom")
    public Question createCustomQuestion(
            @RequestParam Long creatorUserId,
            @RequestBody Question question) {
        return questionService.createCustomQuestion(question, creatorUserId);
    }
    
    /**
     * Get all custom questions by creator
     * GET /questions/custom/creator/{creatorUserId}
     */
    @GetMapping("/custom/creator/{creatorUserId}")
    public List<Question> getCustomQuestionsByCreator(@PathVariable Long creatorUserId) {
        return questionService.getCustomQuestionsByCreator(creatorUserId);
    }
    
    /**
     * Get custom questions by creator and chapter
     * GET /questions/custom/creator/{creatorUserId}/chapter/{chapterId}
     */
    @GetMapping("/custom/creator/{creatorUserId}/chapter/{chapterId}")
    public List<Question> getCustomQuestionsByCreatorAndChapter(
            @PathVariable Long creatorUserId,
            @PathVariable String chapterId) {
        return questionService.getCustomQuestionsByCreatorAndChapter(creatorUserId, chapterId);
    }
    
    /**
     * Update question visibility
     * PUT /questions/{questionId}/visibility?creatorUserId=X
     */
    @PutMapping("/{questionId}/visibility")
    public Question updateQuestionVisibility(
            @PathVariable Long questionId,
            @RequestParam Long creatorUserId,
            @RequestParam String visibility) {
        return questionService.updateQuestionVisibility(questionId, creatorUserId, visibility);
    }
    
    /**
     * Delete a custom question
     * DELETE /questions/custom/{questionId}?creatorUserId=X
     */
    @DeleteMapping("/custom/{questionId}")
    public void deleteCustomQuestion(
            @PathVariable Long questionId,
            @RequestParam Long creatorUserId) {
        questionService.deleteCustomQuestion(questionId, creatorUserId);
    }
    
    /**
     * Get all system questions
     * GET /questions/system
     */
    @GetMapping("/system")
    public List<Question> getSystemQuestions() {
        return questionService.getSystemQuestions();
    }
    
    /**
     * Get all custom questions (admin only)
     * GET /questions/custom
     */
    @GetMapping("/all-custom")
    public List<Question> getAllCustomQuestions() {
        return questionService.getAllCustomQuestions();
    }
}
