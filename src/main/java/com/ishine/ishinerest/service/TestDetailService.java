package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.Question;
import com.ishine.ishinerest.entity.Student;
import com.ishine.ishinerest.entity.TeacherTestQuestion;
import com.ishine.ishinerest.entity.TestAssignment;
import com.ishine.ishinerest.entity.TestDetail;
import com.ishine.ishinerest.pojo.QuestionWithFlagDTO;
import com.ishine.ishinerest.repository.FlaggedQuestionRepository;
import com.ishine.ishinerest.repository.QuestionRepository;
import com.ishine.ishinerest.repository.StudentRepository;
import com.ishine.ishinerest.repository.TestAssignmentRepository;
import com.ishine.ishinerest.repository.TestDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestDetailService {

    private final TestDetailRepository testDetailRepository;
    private final TestAssignmentRepository testAssignmentRepository;
    private final QuestionRepository questionRepository;
    private final StudentRepository studentRepository;
    private final FlaggedQuestionRepository flaggedQuestionRepository;

    public List<TestDetail> getAllTestDetails() {
        return testDetailRepository.findAll();
    }

    public List<TestDetail> getTestDetailsByStudentId(Long studentId) {
        validateStudentExists(studentId);
        return testDetailRepository.findByStudentId(studentId);
    }

    public List<TestDetail> getTestDetailsByStudentAndAssignmentId(Long studentId, Long assignmentId) {
        TestAssignment assignment = validateStudentAssignment(studentId, assignmentId);
        return testDetailRepository.findByStudentIdAndAssignmentId(studentId, assignment.getAssignmentId());
    }

    public List<TestDetail> saveTestDetails(Long studentId, List<TestDetail> testDetails) {
        Student student = validateStudentExists(studentId);
        List<TestDetail> saved = new ArrayList<>();

        for (TestDetail detail : testDetails) {
            Long questionId = detail.getQuestionId();

            if (questionId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "questionId is required");
            }

            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Question not found with id: " + questionId));

            Long assignmentId = detail.getAssignmentId();
            TestAssignment assignment = null;

            Optional<TestDetail> existing;
            if (assignmentId != null) {
                assignment = validateStudentAssignment(studentId, assignmentId);
                validateQuestionBelongsToAssignment(assignment, questionId);
                existing = testDetailRepository.findByStudentIdAndAssignmentIdAndQuestionId(studentId, assignmentId, questionId);
            } else {
                existing = testDetailRepository.findByStudentIdAndAssignmentIsNullAndQuestionId(studentId, questionId);
            }

            if (existing.isPresent()) {
                TestDetail existingDetail = existing.get();
                existingDetail.setStudentAnswer(detail.getStudentAnswer());
                existingDetail.setIsCorrect(detail.getIsCorrect());
                existingDetail.setAttemptCount(existingDetail.getAttemptCount() + 1);
                saved.add(testDetailRepository.save(existingDetail));
            } else {
                detail.setStudent(student);
                detail.setQuestion(question);
                detail.setAssignment(assignment);
                detail.setAttemptCount(1);
                saved.add(testDetailRepository.save(detail));
            }
        }

        return saved;
    }

    public void deleteTestDetail(Long id) {
        testDetailRepository.deleteById(id);
    }

    public void resetStudentProgress(Long studentId) {
        validateStudentExists(studentId);
        testDetailRepository.deleteByStudentId(studentId);
    }

    public void resetAssignmentProgress(Long studentId, Long assignmentId) {
        validateStudentAssignment(studentId, assignmentId);
        testDetailRepository.deleteByStudentIdAndAssignmentId(studentId, assignmentId);
    }

    public List<QuestionWithFlagDTO> getWrongAnswersByAssignmentWithFlags(Long studentId, Long assignmentId) {
        validateStudentAssignment(studentId, assignmentId);

        List<Long> wrongQuestionIds = testDetailRepository
                .findIncorrectlyAnsweredQuestionIdsByStudentAndAssignment(studentId, assignmentId);

        if (wrongQuestionIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Question> wrongQuestions = questionRepository.findAllById(wrongQuestionIds);

        Set<Long> flaggedQuestionIds = flaggedQuestionRepository
                .findByStudent_StudentId(studentId)
                .stream()
                .map(fq -> fq.getQuestion().getQuestionId())
                .collect(Collectors.toSet());

        return wrongQuestions.stream()
                .map(q -> new QuestionWithFlagDTO(q, flaggedQuestionIds.contains(q.getQuestionId())))
                .collect(Collectors.toList());
    }

    private Student validateStudentExists(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Student not found with id: " + studentId));
    }

    private TestAssignment validateStudentAssignment(Long studentId, Long assignmentId) {
        TestAssignment assignment = testAssignmentRepository.findByIdWithDetails(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        if (!assignment.getStudent().getUserId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Assignment " + assignmentId + " does not belong to student " + studentId);
        }

        return assignment;
    }

    private void validateQuestionBelongsToAssignment(TestAssignment assignment, Long questionId) {
        Set<Long> validQuestionIds = assignment.getTest().getTestQuestions().stream()
                .map(TeacherTestQuestion::getQuestion)
                .map(Question::getQuestionId)
                .collect(Collectors.toSet());

        if (!validQuestionIds.contains(questionId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Question " + questionId + " does not belong to assignment " + assignment.getAssignmentId());
        }
    }
}

 // Made with Bob
