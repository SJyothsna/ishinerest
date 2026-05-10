package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.Chapter;
import com.ishine.ishinerest.entity.Question;
import com.ishine.ishinerest.entity.TeacherTest;
import com.ishine.ishinerest.entity.TeacherTestQuestion;
import com.ishine.ishinerest.entity.User;
import com.ishine.ishinerest.entity.UserRole;
import com.ishine.ishinerest.pojo.CreateTeacherTestRequest;
import com.ishine.ishinerest.pojo.TeacherTestResponseDTO;
import com.ishine.ishinerest.repository.ChapterRepository;
import com.ishine.ishinerest.repository.QuestionRepository;
import com.ishine.ishinerest.repository.TeacherSubjectRepository;
import com.ishine.ishinerest.repository.TeacherTestRepository;
import com.ishine.ishinerest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TeacherTestService {

    private final TeacherTestRepository teacherTestRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final ChapterRepository chapterRepository;

    @Transactional
    public TeacherTestResponseDTO createTeacherTest(Long userId, CreateTeacherTestRequest request) {
        // Validate user exists (can be teacher or parent)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        // Only validate subject/chapter permissions for teachers
        if (user.getRole() == UserRole.TEACHER) {
            validateSubjectAndChapter(request.subjectId(), request.chapterId(), userId);
        }

        List<Question> questions = questionRepository.findAllById(request.questionIds());
        if (questions.size() != request.questionIds().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more questionIds are invalid");
        }

        validateQuestionsForUser(userId, user.getRole(), questions, request.subjectId(), request.chapterId());

        TeacherTest teacherTest = new TeacherTest();
        teacherTest.setTitle(request.title());
        teacherTest.setDescription(request.description());
        teacherTest.setSubjectId(request.subjectId());
        teacherTest.setChapterId(request.chapterId());
        teacherTest.setDurationMinutes(request.durationMinutes());
        teacherTest.setIsPublished(request.isPublished());
        teacherTest.setCreatedBy(user);

        List<TeacherTestQuestion> testQuestions = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            TeacherTestQuestion testQuestion = new TeacherTestQuestion();
            testQuestion.setTeacherTest(teacherTest);
            testQuestion.setQuestion(questions.get(i));
            testQuestion.setDisplayOrder(i + 1);
            testQuestions.add(testQuestion);
        }
        teacherTest.setTestQuestions(testQuestions);

        TeacherTest saved = teacherTestRepository.save(teacherTest);
        return TeacherTestResponseDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<TeacherTestResponseDTO> getTeacherTests(Long userId) {
        // Validate user exists (no role restriction)
        userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        return teacherTestRepository.findByCreatedBy_UserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(TeacherTestResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeacherTestResponseDTO getTeacherTestById(Long userId, Long testId) {
        // Validate user exists (no role restriction)
        userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        TeacherTest teacherTest = teacherTestRepository.findByTestIdAndCreatedBy_UserId(testId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found"));
        return TeacherTestResponseDTO.fromEntity(teacherTest);
    }

    /**
     * Get test by ID (public access - no user validation)
     * Only returns published tests
     */
    @Transactional(readOnly = true)
    public TeacherTestResponseDTO getTestByIdPublic(Long testId) {
        TeacherTest teacherTest = teacherTestRepository.findById(testId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found"));
        
        // Only allow access to published tests
        if (!teacherTest.getIsPublished()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Test not found");
        }
        
        return TeacherTestResponseDTO.fromEntity(teacherTest);
    }

    private void validateSubjectAndChapter(String subjectId, String chapterId, Long teacherUserId) {
        if (subjectId != null && !subjectId.isBlank()) {
            boolean allowed = teacherSubjectRepository.existsByTeacherUserIdAndSubjectId(teacherUserId, subjectId);
            if (!allowed) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Teacher is not allowed to create tests for subject: " + subjectId);
            }
        }

        if (chapterId != null && !chapterId.isBlank()) {
            Chapter chapter = chapterRepository.findById(chapterId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid chapterId"));

            boolean allowed = teacherSubjectRepository.existsByTeacherUserIdAndSubjectId(
                    teacherUserId,
                    chapter.getSubject().getSubjectId()
            );
            if (!allowed) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Teacher is not allowed to create tests for chapter: " + chapterId);
            }

            if (subjectId != null && !subjectId.isBlank() && !chapter.getSubject().getSubjectId().equals(subjectId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "chapterId does not belong to subjectId");
            }
        }
    }

    private void validateQuestionsForUser(Long userId, UserRole userRole, List<Question> questions, String subjectId, String chapterId) {
        Set<Long> uniqueQuestionIds = new HashSet<>();
        for (Question question : questions) {
            if (!uniqueQuestionIds.add(question.getQuestionId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate questionIds are not allowed");
            }

            if (question.getChapter() == null || question.getChapter().getSubject() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Question " + question.getQuestionId() + " is missing chapter or subject");
            }

            String questionSubjectId = question.getChapter().getSubject().getSubjectId();
            String questionChapterId = question.getChapter().getChapterId();

            if (subjectId != null && !subjectId.isBlank() && !subjectId.equals(questionSubjectId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Question " + question.getQuestionId() + " does not belong to subjectId " + subjectId);
            }

            if (chapterId != null && !chapterId.isBlank() && !chapterId.equals(questionChapterId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Question " + question.getQuestionId() + " does not belong to chapterId " + chapterId);
            }

            // Only validate subject permissions for teachers
            if (userRole == UserRole.TEACHER) {
                boolean teacherAllowedSubject = teacherSubjectRepository
                        .existsByTeacherUserIdAndSubjectId(userId, questionSubjectId);
                if (!teacherAllowedSubject) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Teacher is not allowed to use question " + question.getQuestionId()
                                    + " for subject " + questionSubjectId);
                }
            }

            boolean isOwnCustomQuestion = question.getCreatedBy() != null
                    && question.getCreatedBy().getUserId().equals(userId);

            boolean isSystemQuestion = question.getCreatedBy() == null
                    || question.getCreatedBy().getRole() == UserRole.ADMIN;

            if (!isOwnCustomQuestion && !isSystemQuestion) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "User can only use admin questions or their own custom questions. Invalid questionId: "
                                + question.getQuestionId());
            }
        }
    }
}

// Made with Bob
