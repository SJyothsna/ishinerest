package com.ishine.ishinerest.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "students")
public class Student {

    @Id
    @Column(name = "student_id")
    private Long studentId;

    // One-to-one relationship with User - student_id IS the user_id
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId  // This makes studentId the same as user.userId
    @JoinColumn(name = "student_id")
    private User user;

    // Make nullable for initial signup (student can pick class later)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = true)
    @JsonBackReference
    @JsonIgnore
    private ClassEntity classEntity;

    // Convenience methods to access user data (backward compatibility)
    // These delegate to the user entity - no duplicate data stored
    public String getName() {
        return user != null ? user.getName() : null;
    }

    public String getEmail() {
        return user != null ? user.getEmail() : null;
    }

    @JsonIgnore
    public String getPasswordHash() {
        return user != null ? user.getPasswordHash() : null;
    }

    // Note: No setters for name/email/passwordHash
    // To update these, modify the user entity directly:
    // student.getUser().setName("New Name");
}
