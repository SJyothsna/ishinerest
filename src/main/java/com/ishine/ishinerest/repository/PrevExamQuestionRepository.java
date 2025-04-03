package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.PrevExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrevExamQuestionRepository extends JpaRepository<PrevExamQuestion, Long> {
    List<PrevExamQuestion> findByChapterId(String chapterId);
}

