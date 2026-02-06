package com.ishine.ishinerest.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "students", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studentId;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    // NEW: store a hashed password (BCrypt)
    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // Make nullable for initial signup (student can pick class later)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = true) // <-- changed to true
    @JsonBackReference
    @JsonIgnore
    private ClassEntity classEntity;
}
