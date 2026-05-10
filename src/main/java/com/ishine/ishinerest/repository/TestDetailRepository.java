package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.TestDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestDetailRepository extends JpaRepository<TestDetail, Long> {

    List<TestDetail> findByStudentId(Long studentId);

    List<TestDetail> findByStudentIdAndAssignmentId(Long studentId, Long assignmentId);

    Optional<TestDetail> findByStudentIdAndAssignmentIdAndQuestionId(Long studentId, Long assignmentId, Long questionId);

    Optional<TestDetail> findByStudentIdAndAssignmentIsNullAndQuestionId(Long studentId, Long questionId);

    void deleteByStudentId(Long studentId);

    void deleteByStudentIdAndAssignmentId(Long studentId, Long assignmentId);

    @Query("""
            SELECT td.questionId
            FROM TestDetail td
            WHERE td.studentId = :studentId
            AND td.assignmentId = :assignmentId
            AND td.isCorrect = false
            """)
    List<Long> findIncorrectlyAnsweredQuestionIdsByStudentAndAssignment(
            @Param("studentId") Long studentId,
            @Param("assignmentId") Long assignmentId);
    @Query(value = """
            SELECT
                (SELECT COUNT(*)
                 FROM questions q
                 JOIN chapters c ON q.chapter_id = c.chapter_id
                 WHERE c.subject_id = :subjectId
                   AND LOWER(COALESCE(q.usage_type, 'both')) IN ('test', 'both')),

                (SELECT COUNT(DISTINCT td.question_id)
                 FROM test_details td
                 JOIN questions q ON td.question_id = q.question_id
                 JOIN chapters c ON q.chapter_id = c.chapter_id
                 WHERE td.student_id = :studentId
                   AND c.subject_id = :subjectId
                   AND LOWER(COALESCE(q.usage_type, 'both')) IN ('test', 'both')),

                (SELECT COUNT(*)
                 FROM test_details td
                 JOIN questions q ON td.question_id = q.question_id
                 JOIN chapters c ON q.chapter_id = c.chapter_id
                 WHERE td.student_id = :studentId
                   AND td.is_correct = true
                   AND c.subject_id = :subjectId
                   AND LOWER(COALESCE(q.usage_type, 'both')) IN ('test', 'both')),

                (SELECT COUNT(*)
                 FROM test_details td
                 JOIN questions q ON td.question_id = q.question_id
                 JOIN chapters c ON q.chapter_id = c.chapter_id
                 WHERE td.student_id = :studentId
                   AND td.is_correct = false
                   AND c.subject_id = :subjectId
                   AND LOWER(COALESCE(q.usage_type, 'both')) IN ('test', 'both'))
            """, nativeQuery = true)
    List<Object[]> getSubjectProgress(@Param("studentId") Long studentId, @Param("subjectId") String subjectId);

    @Query(value = """
            SELECT
                (SELECT COUNT(*)
                 FROM questions q
                 WHERE q.chapter_id = :chapterId
                   AND LOWER(COALESCE(q.usage_type, 'both')) IN ('test', 'both')),

                (SELECT COUNT(DISTINCT td.question_id)
                 FROM test_details td
                 JOIN questions q ON td.question_id = q.question_id
                 WHERE td.student_id = :studentId
                   AND q.chapter_id = :chapterId
                   AND LOWER(COALESCE(q.usage_type, 'both')) IN ('test', 'both')),

                (SELECT COUNT(*)
                 FROM test_details td
                 JOIN questions q ON td.question_id = q.question_id
                 WHERE td.student_id = :studentId
                   AND q.chapter_id = :chapterId
                   AND td.is_correct = true
                   AND LOWER(COALESCE(q.usage_type, 'both')) IN ('test', 'both')),

                (SELECT COUNT(*)
                 FROM test_details td
                 JOIN questions q ON td.question_id = q.question_id
                 WHERE td.student_id = :studentId
                   AND q.chapter_id = :chapterId
                   AND td.is_correct = false
                   AND LOWER(COALESCE(q.usage_type, 'both')) IN ('test', 'both'))
            """, nativeQuery = true)
    List<Object[]> getChapterProgress(@Param("studentId") Long studentId, @Param("chapterId") String chapterId);
}

// Made with Bob
