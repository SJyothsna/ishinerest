package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.TeacherTest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeacherTestRepository extends JpaRepository<TeacherTest, Long> {

    @EntityGraph(attributePaths = {"createdBy", "testQuestions", "testQuestions.question"})
    List<TeacherTest> findByCreatedBy_UserIdOrderByCreatedAtDesc(Long teacherUserId);

    @EntityGraph(attributePaths = {"createdBy", "testQuestions", "testQuestions.question"})
    Optional<TeacherTest> findByTestIdAndCreatedBy_UserId(Long testId, Long teacherUserId);
}

// Made with Bob
