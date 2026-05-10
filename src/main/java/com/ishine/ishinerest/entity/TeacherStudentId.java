package com.ishine.ishinerest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite key for TeacherStudent relationship
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TeacherStudentId implements Serializable {

    @Column(name = "teacher_user_id")
    private Long teacherUserId;

    @Column(name = "student_user_id")
    private Long studentUserId;
}

// Made with Bob
