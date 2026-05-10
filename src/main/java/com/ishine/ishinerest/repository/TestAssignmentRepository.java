package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.TestAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestAssignmentRepository extends JpaRepository<TestAssignment, Long> {

    /**
     * Find all assignments for a specific test
     */
    List<TestAssignment> findByTest_TestIdOrderByAssignedAtDesc(Long testId);

    /**
     * Find all assignments for a specific student
     */
    List<TestAssignment> findByStudent_UserIdOrderByAssignedAtDesc(Long studentUserId);

    /**
     * Find all assignments created by a specific user (teacher/parent)
     */
    List<TestAssignment> findByAssignedBy_UserIdOrderByAssignedAtDesc(Long assignedByUserId);

    /**
     * Find a specific assignment by test and student
     */
    Optional<TestAssignment> findByTest_TestIdAndStudent_UserId(Long testId, Long studentUserId);

    /**
     * Check if a test is already assigned to a student
     */
    boolean existsByTest_TestIdAndStudent_UserId(Long testId, Long studentUserId);

    /**
     * Find assignments by status for a student
     */
    List<TestAssignment> findByStudent_UserIdAndStatusOrderByAssignedAtDesc(Long studentUserId, String status);

    /**
     * Find assignments by test and status
     */
    List<TestAssignment> findByTest_TestIdAndStatusOrderByAssignedAtDesc(Long testId, String status);

    /**
     * Get assignment with full details (test, student, assignedBy)
     */
    @Query("SELECT ta FROM TestAssignment ta " +
           "JOIN FETCH ta.test t " +
           "JOIN FETCH ta.student s " +
           "JOIN FETCH ta.assignedBy ab " +
           "WHERE ta.assignmentId = :assignmentId")
    Optional<TestAssignment> findByIdWithDetails(@Param("assignmentId") Long assignmentId);

    /**
     * Get all assignments for a test with full details
     */
    @Query("SELECT ta FROM TestAssignment ta " +
           "JOIN FETCH ta.test t " +
           "JOIN FETCH ta.student s " +
           "JOIN FETCH ta.assignedBy ab " +
           "WHERE t.testId = :testId " +
           "ORDER BY ta.assignedAt DESC")
    List<TestAssignment> findByTestIdWithDetails(@Param("testId") Long testId);

    /**
     * Get all assignments for a student with full details
     */
    @Query("SELECT ta FROM TestAssignment ta " +
           "JOIN FETCH ta.test t " +
           "JOIN FETCH ta.student s " +
           "JOIN FETCH ta.assignedBy ab " +
           "WHERE s.userId = :studentUserId " +
           "ORDER BY ta.assignedAt DESC")
    List<TestAssignment> findByStudentUserIdWithDetails(@Param("studentUserId") Long studentUserId);
}

// Made with Bob