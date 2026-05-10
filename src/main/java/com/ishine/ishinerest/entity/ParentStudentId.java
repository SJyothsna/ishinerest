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
 * Composite key for ParentStudent relationship
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ParentStudentId implements Serializable {

    @Column(name = "parent_user_id")
    private Long parentUserId;

    @Column(name = "student_user_id")
    private Long studentUserId;
}

// Made with Bob
