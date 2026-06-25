package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.entity.Question;
import com.ishine.ishinerest.pojo.QuestionWithFlagDTO;
import com.ishine.ishinerest.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/questions")
@Tag(name = "Questions", description = "Question management APIs with support for question sets")
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
            question.setQuestionSet(questionDetails.getQuestionSet());

            return questionService.saveQuestion(question);
        }
        return null;
    }

    @DeleteMapping("/{id}")
    public void deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
    }

    // Endpoint for unpracticed questions by subject (NOW WITH isFlagged)
    @Operation(summary = "Get unpracticed questions by subject",
               description = "Returns unpracticed questions for a student filtered by subject, with optional question set filtering for progressive practice")
    @GetMapping("/unpracticed/subject")
    public List<QuestionWithFlagDTO> getUnpracticedQuestionsBySubject(
            @Parameter(description = "Student ID", required = true) @RequestParam Long studentId,
            @Parameter(description = "Subject ID", required = true) @RequestParam String subjectId,
            @Parameter(description = "Maximum number of questions to return", example = "10") @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "Usage type filter (Practice, Test, Both, or 'all' for all types)") @RequestParam(name = "usageType", required = false) String usageType,
            @Parameter(description = "Question set number for progressive practice (1, 2, 3, etc. or 'all' for all sets)", example = "1") @RequestParam(name = "questionSet", required = false) String questionSet) {
        return questionService.getUnpracticedQuestionsBySubjectWithFlags(studentId, subjectId, limit, usageType, questionSet);
    }

    // Endpoint for unpracticed questions by chapter (NOW WITH isFlagged)
    @Operation(summary = "Get unpracticed questions by chapter",
               description = "Returns unpracticed questions for a student filtered by chapter, with optional filters for difficulty, usage type, section, and question set")
    @GetMapping("/unpracticed/chapter")
    public List<QuestionWithFlagDTO> getUnpracticedQuestionsByChapter(
            @Parameter(description = "Student ID", required = true) @RequestParam Long studentId,
            @Parameter(description = "Chapter ID", required = true) @RequestParam String chapterId,
            @Parameter(description = "Maximum number of questions to return", example = "10") @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "Difficulty level filter (Easy, Medium, Hard, or 'all')") @RequestParam(name = "level", required = false) String level,
            @Parameter(description = "Usage type filter (Practice, Test, Both, or 'all')") @RequestParam(name = "usageType", required = false) String usageType,
            @Parameter(description = "Section ID filter") @RequestParam(name = "sectionId", required = false) String sectionId,
            @Parameter(description = "Question set number for progressive practice (1, 2, 3, etc. or 'all' for all sets)", example = "1") @RequestParam(name = "questionSet", required = false) String questionSet) {
        return questionService.getUnpracticedQuestionsByChapterWithFlags(studentId, chapterId, limit, level, usageType, sectionId, questionSet);
    }

    // NEW ENDPOINTS WITH FLAG STATUS

    // Endpoint for unpracticed questions by chapter with flag status
    @Operation(summary = "Get unpracticed questions by chapter with flag status",
               description = "Returns unpracticed questions with flag status, filtered by chapter and optional question set")
    @GetMapping("/unpracticed/chapter/with-flags")
    public List<QuestionWithFlagDTO> getUnpracticedQuestionsByChapterWithFlags(
            @Parameter(description = "Student ID", required = true) @RequestParam Long studentId,
            @Parameter(description = "Chapter ID", required = true) @RequestParam String chapterId,
            @Parameter(description = "Maximum number of questions", example = "10") @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "Difficulty level filter") @RequestParam(name = "level", required = false) String level,
            @Parameter(description = "Usage type filter") @RequestParam(name = "usageType", required = false) String usageType,
            @Parameter(description = "Section ID filter") @RequestParam(name = "sectionId", required = false) String sectionId,
            @Parameter(description = "Question set number (1, 2, 3, etc.)", example = "1") @RequestParam(name = "questionSet", required = false) String questionSet) {
        return questionService.getUnpracticedQuestionsByChapterWithFlags(studentId, chapterId, limit, level, usageType, sectionId, questionSet);
    }

    // Endpoint for unpracticed questions by subject with flag status
    @Operation(summary = "Get unpracticed questions by subject with flag status",
               description = "Returns unpracticed questions with flag status, filtered by subject and optional question set")
    @GetMapping("/unpracticed/subject/with-flags")
    public List<QuestionWithFlagDTO> getUnpracticedQuestionsBySubjectWithFlags(
            @Parameter(description = "Student ID", required = true) @RequestParam Long studentId,
            @Parameter(description = "Subject ID", required = true) @RequestParam String subjectId,
            @Parameter(description = "Maximum number of questions", example = "10") @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "Usage type filter") @RequestParam(name = "usageType", required = false) String usageType,
            @Parameter(description = "Question set number (1, 2, 3, etc.)", example = "1") @RequestParam(name = "questionSet", required = false) String questionSet) {
        return questionService.getUnpracticedQuestionsBySubjectWithFlags(studentId, subjectId, limit, usageType, questionSet);
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
