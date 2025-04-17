package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.PracticeSessionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface PracticeSessionDetailRepository extends JpaRepository<PracticeSessionDetail, Long> {

    List<PracticeSessionDetail> findByStudentId(Long studentId);

    @Query("SELECT psd.questionId FROM PracticeSessionDetail psd WHERE psd.studentId = :studentId")
    List<Long> findIncorrectlyAnsweredQuestionIds(Long studentId);

    @Query("SELECT psd.questionId FROM PracticeSessionDetail psd WHERE psd.studentId = :studentId AND psd.isCorrect = true")
    List<Long> findAnsweredQuestionIds(Long studentId);

    // Fetch answered question IDs by student and chapter
    @Query("""
        SELECT psd.questionId 
        FROM PracticeSessionDetail psd 
        WHERE psd.studentId = :studentId 
        AND psd.chapterId = :chapterId
    """)
    List<Long> findAnsweredQuestionIdsByChapter(Long studentId, String chapterId);
    @Query("""
        SELECT psd.questionId 
        FROM PracticeSessionDetail psd 
        WHERE psd.studentId = :studentId 
        AND psd.chapterId = :chapterId
        AND psd.isCorrect = true
    """)
    List<Long> findCorrectlyAnsweredQuestionIdsByChapter(Long studentId, String chapterId);

    Optional<PracticeSessionDetail> findByStudentIdAndQuestionId(Long studentId, Long questionId);


    @Query(value = """
    SELECT 
        COUNT(*),
        (SELECT COUNT(DISTINCT question_id) FROM practice_session_details 
         WHERE student_id = :studentId AND chapter_id = :chapterId),
        (SELECT COUNT(*) FROM practice_session_details 
         WHERE student_id = :studentId AND chapter_id = :chapterId AND is_correct = true),
        (SELECT COUNT(*) FROM practice_session_details 
         WHERE student_id = :studentId AND chapter_id = :chapterId AND is_correct = false)
    FROM questions WHERE chapter_id = :chapterId
    """, nativeQuery = true)
    List<Object[]> getChapterProgress(@Param("studentId") Long studentId, @Param("chapterId") String chapterId);

    @Query(value = """
    SELECT 
        (SELECT COUNT(*) 
         FROM questions q 
         JOIN chapters c ON q.chapter_id = c.chapter_id 
         WHERE c.subject_id = :subjectId),

        (SELECT COUNT(DISTINCT psd.question_id) 
         FROM practice_session_details psd 
         JOIN questions q ON psd.question_id = q.question_id 
         JOIN chapters c ON q.chapter_id = c.chapter_id 
         WHERE psd.student_id = :studentId AND c.subject_id = :subjectId),

        (SELECT COUNT(*) 
         FROM practice_session_details psd 
         JOIN questions q ON psd.question_id = q.question_id 
         JOIN chapters c ON q.chapter_id = c.chapter_id 
         WHERE psd.student_id = :studentId AND psd.is_correct = true AND c.subject_id = :subjectId),

        (SELECT COUNT(*) 
         FROM practice_session_details psd 
         JOIN questions q ON psd.question_id = q.question_id 
         JOIN chapters c ON q.chapter_id = c.chapter_id 
         WHERE psd.student_id = :studentId AND psd.is_correct = false AND c.subject_id = :subjectId)
    """, nativeQuery = true)
    List<Object[]> getSubjectProgress(@Param("studentId") Long studentId, @Param("subjectId") String subjectId);

}
