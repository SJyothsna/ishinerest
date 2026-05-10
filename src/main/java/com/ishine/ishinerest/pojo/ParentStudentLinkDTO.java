package com.ishine.ishinerest.pojo;

import com.ishine.ishinerest.entity.ParentStudent;
import com.ishine.ishinerest.entity.ParentStudent.LinkStatus;

import java.time.LocalDateTime;

/**
 * DTO for Parent-Student link information
 */
public record ParentStudentLinkDTO(
    Long parentUserId,
    String parentName,
    String parentEmail,
    Long studentUserId,
    String studentName,
    String studentEmail,
    String relationshipType,
    LinkStatus status,
    LocalDateTime createdAt,
    LocalDateTime approvedAt,
    LocalDateTime rejectedAt
) {
    /**
     * Convert ParentStudent entity to DTO
     */
    public static ParentStudentLinkDTO fromEntity(ParentStudent link) {
        return new ParentStudentLinkDTO(
            link.getParent().getUserId(),
            link.getParent().getName(),
            link.getParent().getEmail(),
            link.getStudent().getUserId(),
            link.getStudent().getName(),
            link.getStudent().getEmail(),
            link.getRelationshipType(),
            link.getStatus(),
            link.getCreatedAt(),
            link.getApprovedAt(),
            link.getRejectedAt()
        );
    }
}

// Made with Bob