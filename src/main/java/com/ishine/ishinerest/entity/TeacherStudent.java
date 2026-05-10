package com.ishine.ishinerest.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing the relationship between a teacher and a student
 */
@Getter
@Setter
@Entity
@Table(name = "teacher_student")
public class TeacherStudent {

    @EmbeddedId
    private TeacherStudentId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("teacherUserId")
    @JoinColumn(name = "teacher_user_id")
    private User teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("studentUserId")
    @JoinColumn(name = "student_user_id")
    private User student;

    @Column(length = 50)
    private String subjectId;  // Optional: which subject the teacher teaches this student

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

// Made with Bob
