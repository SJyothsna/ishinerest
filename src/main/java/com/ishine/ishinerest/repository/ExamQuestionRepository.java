package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.ExamQuestion;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {
    List<ExamQuestion> findByChapter_ChapterId(String chapterId);  // Find by Chapter's ID
}
