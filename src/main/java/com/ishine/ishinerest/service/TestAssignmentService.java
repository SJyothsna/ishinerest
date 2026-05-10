package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.ParentStudent.LinkStatus;
import com.ishine.ishinerest.entity.TeacherTest;
import com.ishine.ishinerest.entity.TestAssignment;
import com.ishine.ishinerest.entity.User;
import com.ishine.ishinerest.entity.UserRole;
import com.ishine.ishinerest.pojo.AssignTestRequest;
import com.ishine.ishinerest.pojo.TestAssignmentDTO;
import com.ishine.ishinerest.repository.ParentStudentRepository;
import com.ishine.ishinerest.repository.TeacherStudentRepository;
import com.ishine.ishinerest.repository.TeacherTestRepository;
import com.ishine.ishinerest.repository.TestAssignmentRepository;
import com.ishine.ishinerest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestAssignmentService {

    private final TestAssignmentRepository testAssignmentRepository;
    private final TeacherTestRepository teacherTestRepository;
    private final UserRepository userRepository;
    private final TeacherStudentRepository teacherStudentRepository;
    private final ParentStudentRepository parentStudentRepository;

    /**
     * Assign a test to multiple students
     */
    @Transactional
    public List<TestAssignmentDTO> assignTest(Long userId, AssignTestRequest request) {
        // Validate user exists
        User assignedBy = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Validate test exists and belongs to the user
        TeacherTest test = teacherTestRepository.findByTestIdAndCreatedBy_UserId(request.testId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Test not found or you don't have permission to assign it"));

        // Validate test is published
        if (!test.getIsPublished()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Cannot assign unpublished test. Please publish the test first.");
        }

        List<TestAssignment> assignments = new ArrayList<>();

        for (Long studentUserId : request.studentUserIds()) {
            // Validate student exists and has STUDENT role
            User student = userRepository.findById(studentUserId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                            "Student with userId " + studentUserId + " not found"));

            if (student.getRole() != UserRole.STUDENT) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                        "User " + studentUserId + " is not a student");
            }

            // Validate relationship between assigner and student
            validateRelationship(assignedBy, student);

            // Check if already assigned
            if (testAssignmentRepository.existsByTest_TestIdAndStudent_UserId(request.testId(), studentUserId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                        "Test is already assigned to student " + studentUserId);
            }

            // Create assignment
            TestAssignment assignment = new TestAssignment();
            assignment.setTest(test);
            assignment.setStudent(student);
            assignment.setAssignedBy(assignedBy);
            assignment.setDueDate(request.dueDate());
            assignment.setStatus("ASSIGNED");

            assignments.add(assignment);
        }

        // Save all assignments
        List<TestAssignment> saved = testAssignmentRepository.saveAll(assignments);
        
        return saved.stream()
                .map(TestAssignmentDTO::fromEntity)
                .toList();
    }

    /**
     * Get all assignments for a specific test
     */
    @Transactional(readOnly = true)
    public List<TestAssignmentDTO> getTestAssignments(Long userId, Long testId) {
        // Validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Validate test exists and belongs to the user
        teacherTestRepository.findByTestIdAndCreatedBy_UserId(testId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Test not found or you don't have permission to view its assignments"));

        return testAssignmentRepository.findByTestIdWithDetails(testId)
                .stream()
                .map(TestAssignmentDTO::fromEntity)
                .toList();
    }

    /**
     * Get all tests assigned to a student
     */
    @Transactional(readOnly = true)
    public List<TestAssignmentDTO> getStudentAssignments(Long studentUserId) {
        // Validate student exists
        User student = userRepository.findById(studentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        if (student.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a student");
        }

        return testAssignmentRepository.findByStudentUserIdWithDetails(studentUserId)
                .stream()
                .map(TestAssignmentDTO::fromEntity)
                .toList();
    }

    /**
     * Get a specific assignment by ID
     */
    @Transactional(readOnly = true)
    public TestAssignmentDTO getAssignmentById(Long assignmentId) {
        TestAssignment assignment = testAssignmentRepository.findByIdWithDetails(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        return TestAssignmentDTO.fromEntity(assignment);
    }

    /**
     * Start a test (student marks as in progress)
     */
    @Transactional
    public TestAssignmentDTO startTest(Long studentUserId, Long assignmentId) {
        TestAssignment assignment = testAssignmentRepository.findByIdWithDetails(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        // Validate student owns this assignment
        if (!assignment.getStudent().getUserId().equals(studentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "You don't have permission to start this test");
        }

        // Validate status
        if (!assignment.getStatus().equals("ASSIGNED")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Test has already been started or completed");
        }

        assignment.setStatus("IN_PROGRESS");
        assignment.setStartedAt(LocalDateTime.now());

        TestAssignment saved = testAssignmentRepository.save(assignment);
        return TestAssignmentDTO.fromEntity(saved);
    }

    /**
     * Complete a test (student submits)
     */
    @Transactional
    public TestAssignmentDTO completeTest(Long studentUserId, Long assignmentId, Integer score) {
        TestAssignment assignment = testAssignmentRepository.findByIdWithDetails(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        // Validate student owns this assignment
        if (!assignment.getStudent().getUserId().equals(studentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "You don't have permission to complete this test");
        }

        // Validate status
        if (assignment.getStatus().equals("COMPLETED")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Test is already completed");
        }

        // Validate score
        if (score != null && (score < 0 || score > 100)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Score must be between 0 and 100");
        }

        assignment.setStatus("COMPLETED");
        assignment.setCompletedAt(LocalDateTime.now());
        assignment.setScore(score);

        TestAssignment saved = testAssignmentRepository.save(assignment);
        return TestAssignmentDTO.fromEntity(saved);
    }

    /**
     * Delete an assignment
     */
    @Transactional
    public void deleteAssignment(Long userId, Long assignmentId) {
        TestAssignment assignment = testAssignmentRepository.findByIdWithDetails(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        // Validate user created this assignment
        if (!assignment.getAssignedBy().getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "You don't have permission to delete this assignment");
        }

        testAssignmentRepository.delete(assignment);
    }

    /**
     * Validate relationship between teacher/parent and student
     */
    private void validateRelationship(User assignedBy, User student) {
        Long assignedByUserId = assignedBy.getUserId();
        Long studentUserId = student.getUserId();

        if (assignedBy.getRole() == UserRole.TEACHER) {
            boolean hasRelationship = teacherStudentRepository
                    .existsByTeacher_UserIdAndStudent_UserId(assignedByUserId, studentUserId);
            if (!hasRelationship) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You don't have a relationship with student " + studentUserId);
            }
        } else if (assignedBy.getRole() == UserRole.PARENT) {
            boolean hasRelationship = parentStudentRepository
                    .existsByParent_UserIdAndStudent_UserIdAndStatus(assignedByUserId, studentUserId, LinkStatus.ACTIVE);
            if (!hasRelationship) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You don't have an active relationship with student " + studentUserId);
            }
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only teachers and parents can assign tests");
        }
    }
}

// Made with Bob