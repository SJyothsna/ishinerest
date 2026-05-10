package com.ishine.ishinerest.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing the relationship between a parent and a student
 */
@Getter
@Setter
@Entity
@Table(name = "parent_student")
public class ParentStudent {

    @EmbeddedId
    private ParentStudentId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("parentUserId")
    @JoinColumn(name = "parent_user_id")
    private User parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("studentUserId")
    @JoinColumn(name = "student_user_id")
    private User student;

    @Column(length = 50)
    private String relationshipType;  // MOTHER, FATHER, GUARDIAN, etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LinkStatus status = LinkStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime approvedAt;

    @Column
    private LocalDateTime rejectedAt;

    /**
     * Status of the parent-student link
     */
    public enum LinkStatus {
        PENDING,   // Waiting for student approval
        ACTIVE,    // Approved and active
        REJECTED,  // Student rejected the request
        REVOKED    // Link was removed by parent or student
    }
}

// Made with Bob
