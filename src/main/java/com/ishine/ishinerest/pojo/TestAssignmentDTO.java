package com.ishine.ishinerest.pojo;

import com.ishine.ishinerest.entity.TestAssignment;

import java.time.LocalDateTime;

public record TestAssignmentDTO(
        Long assignmentId,
        Long testId,
        String testTitle,
        Long studentUserId,
        String studentName,
        String studentEmail,
        Long assignedByUserId,
        String assignedByName,
        LocalDateTime assignedAt,
        LocalDateTime dueDate,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        Integer score,
        String status,
        String feedback
) {
    public static TestAssignmentDTO fromEntity(TestAssignment assignment) {
        return new TestAssignmentDTO(
                assignment.getAssignmentId(),
                assignment.getTest().getTestId(),
                assignment.getTest().getTitle(),
                assignment.getStudent().getUserId(),
                assignment.getStudent().getName(),
                assignment.getStudent().getEmail(),
                assignment.getAssignedBy().getUserId(),
                assignment.getAssignedBy().getName(),
                assignment.getAssignedAt(),
                assignment.getDueDate(),
                assignment.getStartedAt(),
                assignment.getCompletedAt(),
                assignment.getScore(),
                assignment.getStatus(),
                assignment.getFeedback()
        );
    }
}

// Made with Bob