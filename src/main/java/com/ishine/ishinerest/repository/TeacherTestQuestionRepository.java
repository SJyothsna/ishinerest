package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.TeacherTestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherTestQuestionRepository extends JpaRepository<TeacherTestQuestion, Long> {
    void deleteByQuestion_QuestionId(Long questionId);
}

// Made with Bob
