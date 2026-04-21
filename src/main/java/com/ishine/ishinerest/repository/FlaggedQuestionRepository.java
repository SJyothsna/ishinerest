package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.FlaggedQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FlaggedQuestionRepository extends JpaRepository<FlaggedQuestion, Long> {

    // Find a specific flagged question by student and question
    Optional<FlaggedQuestion> findByStudent_StudentIdAndQuestion_QuestionId(Long studentId, Long questionId);

    // Find all flagged questions for a student in a specific chapter
    @Query("SELECT fq FROM FlaggedQuestion fq " +
            "WHERE fq.student.studentId = :studentId " +
            "AND fq.question.chapter.chapterId = :chapterId")
    List<FlaggedQuestion> findByStudentIdAndChapterId(
            @Param("studentId") Long studentId,
            @Param("chapterId") String chapterId);

    // Find all flagged questions for a student
    List<FlaggedQuestion> findByStudent_StudentId(Long studentId);

    // Delete a specific flagged question
    void deleteByStudent_StudentIdAndQuestion_QuestionId(Long studentId, Long questionId);
}

// Made with Bob
