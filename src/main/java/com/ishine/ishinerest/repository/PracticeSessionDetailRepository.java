package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.PracticeSessionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PracticeSessionDetailRepository extends JpaRepository<PracticeSessionDetail, Long> {
    @Query("SELECT psd.questionId FROM PracticeSessionDetail psd WHERE psd.student.studentId = :studentId AND psd.isCorrect = false")
    List<Long> findIncorrectlyAnsweredQuestionIds(Long studentId);

    @Query("SELECT psd.questionId FROM PracticeSessionDetail psd WHERE psd.student.studentId = :studentId AND psd.isCorrect = true")
    List<Long> findAnsweredQuestionIds(Long studentId);

    // Fetch answered question IDs by student and chapter
    @Query("""
        SELECT psd.questionId 
        FROM PracticeSessionDetail psd 
        WHERE psd.student.studentId = :studentId 
        AND psd.chapterId = :chapterId
    """)
    List<Long> findAnsweredQuestionIdsByChapter(Long studentId, Long chapterId);
    @Query("""
        SELECT psd.questionId 
        FROM PracticeSessionDetail psd 
        WHERE psd.student.studentId = :studentId 
        AND psd.chapterId = :chapterId
        AND psd.isCorrect = true
    """)
    List<Long> findCorrectlyAnsweredQuestionIdsByChapter(Long studentId, Long chapterId);
}
