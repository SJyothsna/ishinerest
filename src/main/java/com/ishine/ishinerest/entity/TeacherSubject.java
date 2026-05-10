package com.ishine.ishinerest.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "teacher_subject")
@Getter
@Setter
public class TeacherSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long teacherUserId;

    private String subjectId;

    private LocalDateTime selectedAt = LocalDateTime.now();
}

// Made with Bob
