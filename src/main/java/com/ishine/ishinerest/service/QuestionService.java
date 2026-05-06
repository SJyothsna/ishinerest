package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.Chapter;
import com.ishine.ishinerest.entity.Question;
import com.ishine.ishinerest.entity.User;
import com.ishine.ishinerest.entity.UserRole;
import com.ishine.ishinerest.pojo.QuestionWithFlagDTO;
import com.ishine.ishinerest.repository.ChapterRepository;
import com.ishine.ishinerest.repository.FlaggedQuestionRepository;
import com.ishine.ishinerest.repository.QuestionRepository;
import com.ishine.ishinerest.repository.PracticeSessionDetailRepository;
import com.ishine.ishinerest.repository.TeacherSubjectRepository;
import com.ishine.ishinerest.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    @Autowired
    private PracticeSessionDetailRepository practiceSessionDetailRepository;
    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private FlaggedQuestionRepository flaggedQuestionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherSubjectRepository teacherSubjectRepository;

    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    public List<Question> getQuestionsByCreatorOrDefault(Long createdByUserId) {
        if (createdByUserId != null) {
            var creator = userRepository.findById(createdByUserId)
                    .orElseThrow(() -> new RuntimeException("Creator not found"));
            return questionRepository.findByCreatedBy(creator);
        }

        User adminUser = userRepository.findByRole(UserRole.ADMIN)
                .stream()
                .findFirst()
                .orElse(null);

        if (adminUser != null) {
            return questionRepository.findByCreatedBy(adminUser);
        }

        return questionRepository.findByCreatedByIsNull();
    }

    public Optional<Question> getQuestionById(Long id) {
        return questionRepository.findById(id);
    }

    public List<Question> getQuestionsByChapter(String chapterId) {
        return questionRepository.findByChapter_ChapterId(chapterId);
    }

    public List<Question> getQuestionsBySubject(String subjectId) {
        return questionRepository.findBySubjectId(subjectId);
    }

    public Question saveQuestion(Question question) {
        applyCreatedByRule(question);
        validateQuestion(question);
        normalizeCorrectAnswers(question);
        return questionRepository.save(question);
    }

    public List<Question> saveQuestions(List<Question> questions) {
        questions.forEach(this::applyCreatedByRule);
        questions.forEach(this::validateQuestion);
        questions.forEach(this::normalizeCorrectAnswers);
        return questionRepository.saveAll(questions);
    }

    public List<Question> saveQuestionsFromExcel(MultipartFile file) {
        List<Question> questions = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
                Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheet("Questions");

            for (Row row : sheet) {
                if (row.getRowNum() == 0)
                    continue; // Skip header row

                Question question = new Question();
                // QuestionId is auto-generated, so we don't set it from Excel
                String chapterId = getCellValue(row.getCell(0));

                // Skip empty rows
                if (chapterId == null || chapterId.trim().isEmpty()) {
                    continue;
                }

                question.setQuestionText(getCellValue(row.getCell(1)));
                question.setCorrectAnswer(getCellValue(row.getCell(2)));
                question.setOptionA(getCellValue(row.getCell(3)));
                question.setOptionB(getCellValue(row.getCell(4)));
                question.setOptionC(getCellValue(row.getCell(5)));
                question.setOptionD(getCellValue(row.getCell(6)));
                question.setOptionE(getCellValue(row.getCell(7)));
                question.setOptionF(getCellValue(row.getCell(8)));
                question.setExplanation(getCellValue(row.getCell(9)));
                question.setDifficultyLevel(getCellValue(row.getCell(10)));

                // Handle QuestionType - default to 1 if empty
                String questionTypeStr = getCellValue(row.getCell(11));
                question.setQuestionType(questionTypeStr != null && !questionTypeStr.trim().isEmpty()
                        ? Integer.parseInt(questionTypeStr.trim())
                        : 1);

                question.setCorrectAnswers(getCellValue(row.getCell(12)));
                question.setNotes(getCellValue(row.getCell(13)));
                
                // Handle UsageType - default to "Both" if empty
                String usageType = getCellValue(row.getCell(14));
                question.setUsageType(usageType != null && !usageType.trim().isEmpty()
                        ? usageType.trim()
                        : "Both");

                Chapter chapter = chapterRepository.findById(chapterId)
                        .orElseThrow(() -> new RuntimeException("Chapter not found for ID: " + chapterId));

                question.setChapter(chapter);

                applyCreatedByRule(question);
                questions.add(question);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage());
        }

        return questionRepository.saveAll(questions);
    }

    // Helper method to get cell values as strings
    private String getCellValue(Cell cell) {
        if (cell == null)
            return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    public void deleteQuestion(Long id) {
        // Get the question first to check if it has an image
        Optional<Question> questionOpt = questionRepository.findById(id);
        
        if (questionOpt.isPresent()) {
            Question question = questionOpt.get();
            String imageUrl = question.getQuestionImageUrl();
            
            // Delete the image file if it exists
            if (imageUrl != null && !imageUrl.isEmpty()) {
                deleteImageFile(imageUrl);
            }
        }
        
        // Delete the question from database
        questionRepository.deleteById(id);
    }
    
    private void deleteImageFile(String imageUrl) {
        try {
            // Extract path from URL
            // Example: http://localhost:8080/uploads/chapters/LC5H0102/questions/q_123.png
            // Extract: public/uploads/chapters/LC5H0102/questions/q_123.png
            String baseUrl = "http://localhost:8080/uploads/chapters";
            String baseDir = "public/uploads/chapters";
            
            if (imageUrl.startsWith(baseUrl)) {
                String relativePath = imageUrl.replace(baseUrl, baseDir);
                java.io.File file = new java.io.File(relativePath);
                
                if (file.exists()) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        System.out.println("Deleted image file: " + relativePath);
                    } else {
                        System.err.println("Failed to delete image file: " + relativePath);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error deleting image file: " + e.getMessage());
            // Don't throw exception - we still want to delete the question even if image deletion fails
        }
    }

    // Unpracticed questions by subject
    public List<Question> getUnpracticedQuestionsBySubject(Long studentId, String subjectId, int limit, String usageType) {
        // Normalize usageType parameter - treat "all" as null to get all usage types
        String normalizedUsageType = (usageType != null && usageType.equalsIgnoreCase("all")) ? null : usageType;
        
        // Get practiced question IDs for the student
        List<Long> practicedQuestionIds = practiceSessionDetailRepository.findAnsweredQuestionIds(studentId);

        // If no questions have been practiced, return questions with limit
        if (practicedQuestionIds == null || practicedQuestionIds.isEmpty()) {
            return questionRepository.findBySubjectIdWithLimit(subjectId, limit, normalizedUsageType);
        }

        // Get unpracticed questions for the subject
        return questionRepository.findUnpracticedQuestionsBySubject(subjectId, practicedQuestionIds, limit, normalizedUsageType);
    }

    // Unpracticed questions by chapter
    public List<Question> getUnpracticedQuestionsByChapter(Long studentId, String chapterId, int limit, String level, String usageType) {
        // Normalize level parameter - treat "all" as null to get all difficulty levels
        String normalizedLevel = (level != null && level.equalsIgnoreCase("all")) ? null : level;
        
        // Normalize usageType parameter - treat "all" as null to get all usage types
        String normalizedUsageType = (usageType != null && usageType.equalsIgnoreCase("all")) ? null : usageType;

        // Get answered question IDs for the student and chapter
        List<Long> practicedQuestionIds = practiceSessionDetailRepository
                .findCorrectlyAnsweredQuestionIdsByChapter(studentId, chapterId);

        if (practicedQuestionIds == null || practicedQuestionIds.isEmpty()) {
            return questionRepository.findByChapterIdWithLimit(chapterId, limit, normalizedUsageType);
        }
        // Get unpracticed questions for the chapter
        return questionRepository.findUnpracticedQuestionsByChapter(chapterId, practicedQuestionIds, limit,
                normalizedLevel, normalizedUsageType);
    }

    // Get unpracticed questions by chapter with flag status
    public List<QuestionWithFlagDTO> getUnpracticedQuestionsByChapterWithFlags(
            Long studentId, String chapterId, int limit, String level, String usageType) {
        List<Question> questions = getUnpracticedQuestionsByChapter(studentId, chapterId, limit, level, usageType);
        return addFlagStatusToQuestions(questions, studentId);
    }

    // Get unpracticed questions by subject with flag status
    public List<QuestionWithFlagDTO> getUnpracticedQuestionsBySubjectWithFlags(
            Long studentId, String subjectId, int limit, String usageType) {
        List<Question> questions = getUnpracticedQuestionsBySubject(studentId, subjectId, limit, usageType);
        return addFlagStatusToQuestions(questions, studentId);
    }

    // Get questions by chapter with flag status
    public List<QuestionWithFlagDTO> getQuestionsByChapterWithFlags(String chapterId, Long studentId) {
        List<Question> questions = getQuestionsByChapter(chapterId);
        return addFlagStatusToQuestions(questions, studentId);
    }

    // Get questions by subject with flag status
    public List<QuestionWithFlagDTO> getQuestionsBySubjectWithFlags(String subjectId, Long studentId) {
        List<Question> questions = getQuestionsBySubject(subjectId);
        return addFlagStatusToQuestions(questions, studentId);
    }

    /**
     * Get questions that were answered incorrectly and haven't been correctly
     * answered yet (by chapter)
     * These are questions the student got wrong and still need to practice
     */
    public List<QuestionWithFlagDTO> getWrongUnpracticedQuestionsByChapter(Long studentId, String chapterId) {
        // Get all incorrectly answered question IDs for this chapter
        List<Long> incorrectQuestionIds = practiceSessionDetailRepository
                .findIncorrectlyAnsweredQuestionIdsByChapter(studentId, chapterId);

        if (incorrectQuestionIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Get all correctly answered question IDs for this chapter
        Set<Long> correctQuestionIds = practiceSessionDetailRepository
                .findCorrectlyAnsweredQuestionIdsByChapter(studentId, chapterId)
                .stream()
                .collect(Collectors.toSet());

        // Filter out questions that have been correctly answered
        List<Long> wrongUnpracticedIds = incorrectQuestionIds.stream()
                .filter(id -> !correctQuestionIds.contains(id))
                .distinct()
                .collect(Collectors.toList());

        if (wrongUnpracticedIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Fetch the questions
        List<Question> questions = questionRepository.findAllById(wrongUnpracticedIds);

        // Add flag status and return
        return addFlagStatusToQuestions(questions, studentId);
    }

    /**
     * Get questions that were answered incorrectly and haven't been correctly
     * answered yet (by subject)
     * These are questions the student got wrong and still need to practice
     */
    public List<QuestionWithFlagDTO> getWrongUnpracticedQuestionsBySubject(Long studentId, String subjectId) {
        // Get all incorrectly answered question IDs for this subject
        List<Long> incorrectQuestionIds = practiceSessionDetailRepository
                .findIncorrectlyAnsweredQuestionIdsBySubject(studentId, subjectId);

        if (incorrectQuestionIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Get all correctly answered question IDs for this student (across all
        // subjects)
        Set<Long> correctQuestionIds = practiceSessionDetailRepository
                .findAnsweredQuestionIds(studentId)
                .stream()
                .filter(id -> {
                    // Check if this question was answered correctly
                    return practiceSessionDetailRepository
                            .findByStudentIdAndQuestionId(studentId, id)
                            .map(psd -> psd.getIsCorrect())
                            .orElse(false);
                })
                .collect(Collectors.toSet());

        // Filter out questions that have been correctly answered
        List<Long> wrongUnpracticedIds = incorrectQuestionIds.stream()
                .filter(id -> !correctQuestionIds.contains(id))
                .distinct()
                .collect(Collectors.toList());

        if (wrongUnpracticedIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Fetch the questions
        List<Question> questions = questionRepository.findAllById(wrongUnpracticedIds);

        // Add flag status and return
        return addFlagStatusToQuestions(questions, studentId);
    }

    // Helper method to add flag status to questions
    private List<QuestionWithFlagDTO> addFlagStatusToQuestions(List<Question> questions, Long studentId) {
        // Get all flagged question IDs for this student
        Set<Long> flaggedQuestionIds = flaggedQuestionRepository
                .findByStudent_StudentId(studentId)
                .stream()
                .map(fq -> fq.getQuestion().getQuestionId())
                .collect(Collectors.toSet());

        // Convert questions to DTOs with flag status
        return questions.stream()
                .map(q -> new QuestionWithFlagDTO(q, flaggedQuestionIds.contains(q.getQuestionId())))
                .collect(Collectors.toList());
    }

    private void normalizeCorrectAnswers(Question question) {
        String correctAnswer = question.getCorrectAnswer();
        String correctAnswers = question.getCorrectAnswers();

        if (correctAnswer != null) {
            correctAnswer = correctAnswer.trim();
            question.setCorrectAnswer(correctAnswer.isEmpty() ? null : correctAnswer);
        }

        if (correctAnswers != null) {
            correctAnswers = correctAnswers.trim();
            question.setCorrectAnswers(correctAnswers.isEmpty() ? null : correctAnswers);
        }

        if (question.getQuestionType() == 4) {
            if ((question.getCorrectAnswers() == null || question.getCorrectAnswers().isBlank())
                    && question.getCorrectAnswer() != null && !question.getCorrectAnswer().isBlank()) {
                question.setCorrectAnswers(question.getCorrectAnswer());
            }
            if (question.getCorrectAnswer() == null) {
                question.setCorrectAnswer("");
            }
        } else {
            question.setCorrectAnswers(question.getCorrectAnswers());
        }
    }

    /**
     * Validates that a question has either questionText or questionImageUrl
     * At least one must be provided
     * Note: This validation allows null/empty questionText during initial save
     * because the image upload happens after question creation
     */
    private void validateQuestion(Question question) {
        String questionText = question.getQuestionText();
        String questionImageUrl = question.getQuestionImageUrl();
        
        boolean hasText = questionText != null && !questionText.trim().isEmpty();
        boolean hasImage = questionImageUrl != null && !questionImageUrl.trim().isEmpty();
        
        // Allow questions without text if they will have an image uploaded later
        // The frontend ensures at least one is provided before submission
        // For updates, we validate that at least one exists
        if (question.getQuestionId() != null && !hasText && !hasImage) {
            // This is an update operation - both text and image are missing
            throw new IllegalArgumentException("Question must have either question text or question image");
        }
        // For new questions (questionId is null), we allow empty text as image will be uploaded next
    }

    // public List<Question> getQuestionsByChapter(Long chapterId) {
    // return questionRepository.findQuestionsByChapterExcludingPractice(chapterId);
    // }
    
    // ========================================================================
    // CREATOR TRACKING METHODS (for custom questions by parents/teachers)
    // ========================================================================
    
    /**
     * Create a custom question by a parent or teacher
     * @param question The question to create
     * @param creatorUserId The user ID of the creator (parent or teacher)
     * @return The saved question
     */
    public Question createCustomQuestion(Question question, Long creatorUserId) {
        // Verify creator exists and has appropriate role
        var creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new RuntimeException("Creator not found"));
        
        if (creator.getRole() != com.ishine.ishinerest.entity.UserRole.PARENT &&
            creator.getRole() != com.ishine.ishinerest.entity.UserRole.TEACHER) {
            throw new RuntimeException("Only parents and teachers can create custom questions");
        }

        if (creator.getRole() == com.ishine.ishinerest.entity.UserRole.TEACHER) {
            if (question.getChapter() == null || question.getChapter().getChapterId() == null) {
                throw new RuntimeException("Chapter is required for teacher custom questions");
            }

            var chapter = chapterRepository.findById(question.getChapter().getChapterId())
                    .orElseThrow(() -> new RuntimeException("Chapter not found"));

            String subjectId = chapter.getSubject().getSubjectId();
            boolean teacherHasSubject = teacherSubjectRepository
                    .existsByTeacherUserIdAndSubjectId(creatorUserId, subjectId);

            if (!teacherHasSubject) {
                throw new RuntimeException("Teacher is not allowed to create questions for subject: " + subjectId);
            }

            question.setChapter(chapter);
        }
        
        // Set creator and custom flags
        question.setCreatedBy(creator);
        question.setIsCustom(true);
        question.setVisibility("PRIVATE"); // Default to private
        
        // Validate and save
        validateQuestion(question);
        normalizeCorrectAnswers(question);
        return questionRepository.save(question);
    }
    
    /**
     * Get all custom questions created by a user
     */
    public List<Question> getCustomQuestionsByCreator(Long creatorUserId) {
        return getQuestionsByCreatorOrDefault(creatorUserId);
    }
    
    /**
     * Get all custom questions for a specific chapter created by a user
     */
    public List<Question> getCustomQuestionsByCreatorAndChapter(Long creatorUserId, String chapterId) {
        if (creatorUserId != null) {
            var creator = userRepository.findById(creatorUserId)
                    .orElseThrow(() -> new RuntimeException("Creator not found"));
            return questionRepository.findByCreatedByAndChapter_ChapterId(creator, chapterId);
        }

        User adminUser = userRepository.findByRole(UserRole.ADMIN)
                .stream()
                .findFirst()
                .orElse(null);

        if (adminUser != null) {
            return questionRepository.findByCreatedByAndChapter_ChapterId(adminUser, chapterId);
        }

        return questionRepository.findByCreatedByIsNullAndChapter_ChapterId(chapterId);
    }
    
    /**
     * Update visibility of a custom question
     */
    public Question updateQuestionVisibility(Long questionId, Long creatorUserId, String visibility) {
        var question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        
        // Verify the user is the creator
        if (question.getCreatedBy() == null || 
            !question.getCreatedBy().getUserId().equals(creatorUserId)) {
            throw new RuntimeException("Only the creator can update question visibility");
        }
        
        // Validate visibility value
        if (!visibility.equals("PUBLIC") && !visibility.equals("PRIVATE") && !visibility.equals("SHARED")) {
            throw new RuntimeException("Invalid visibility value. Must be PUBLIC, PRIVATE, or SHARED");
        }
        
        question.setVisibility(visibility);
        return questionRepository.save(question);
    }
    
    /**
     * Delete a custom question (only by creator)
     */
    public void deleteCustomQuestion(Long questionId, Long creatorUserId) {
        var question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        
        // Verify the user is the creator
        if (question.getCreatedBy() == null || 
            !question.getCreatedBy().getUserId().equals(creatorUserId)) {
            throw new RuntimeException("Only the creator can delete their custom questions");
        }
        
        // Verify it's a custom question
        if (!question.getIsCustom()) {
            throw new RuntimeException("Cannot delete system questions");
        }
        
        deleteQuestion(questionId);
    }
    
    /**
     * Get all system questions (not custom)
     */
    public List<Question> getSystemQuestions() {
        return questionRepository.findByIsCustom(false);
    }
    
    /**
     * Get all custom questions
     */
    public List<Question> getAllCustomQuestions() {
        return questionRepository.findByIsCustom(true);
    }

    private void applyCreatedByRule(Question question) {
        if (question.getCreatedBy() != null && question.getCreatedBy().getUserId() != null) {
            User providedCreator = userRepository.findById(question.getCreatedBy().getUserId())
                    .orElseThrow(() -> new RuntimeException("CreatedBy user not found: " + question.getCreatedBy().getUserId()));
            question.setCreatedBy(providedCreator);
            return;
        }

        User adminUser = userRepository.findByRole(UserRole.ADMIN)
                .stream()
                .findFirst()
                .orElse(null);

        question.setCreatedBy(adminUser);
    }
}
