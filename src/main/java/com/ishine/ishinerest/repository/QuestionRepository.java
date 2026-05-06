package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.Question;
import com.ishine.ishinerest.entity.User;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByChapter_ChapterId(String chapterId);

    @Query("SELECT q FROM Question q WHERE q.chapter.subject.subjectId = :subjectId")
    List<Question> findBySubjectId(@Param("subjectId") String subjectId);

    @Query("SELECT q FROM Question q WHERE q.chapter.chapterId = :chapterId " +
            "AND q.questionId NOT IN (" +
            "   SELECT psd.questionId FROM PracticeSessionDetail psd " +
            "   WHERE psd.isCorrect = true" +
            ")")
    List<Question> findQuestionsByChapterExcludingPractice(@Param("chapterId") String chapterId);

    // Fetch unpracticed questions by subject
    @Query("""
                SELECT q FROM Question q
                WHERE q.chapter.subject.subjectId = :subjectId
                AND q.questionId NOT IN :practicedQuestionIds
                AND (:usageType IS NULL OR :usageType = '' OR q.usageType = :usageType OR q.usageType = 'Both')
                ORDER BY q.questionId ASC
                LIMIT :limit
            """)
    List<Question> findUnpracticedQuestionsBySubject(String subjectId, List<Long> practicedQuestionIds, int limit, String usageType);

    // Fetch questions by subject with limit (when no practiced questions)
    @Query(value = """
                SELECT q.* FROM questions q
                JOIN chapters c ON q.chapter_id = c.chapter_id
                WHERE c.subject_id = :subjectId
                AND (:usageType IS NULL OR :usageType = '' OR q.usage_type = :usageType OR q.usage_type = 'Both')
                ORDER BY q.question_id ASC
                LIMIT :limit
            """, nativeQuery = true)
    List<Question> findBySubjectIdWithLimit(@Param("subjectId") String subjectId, @Param("limit") int limit, @Param("usageType") String usageType);

    // Fetch unpracticed questions by chapter
    @Query("""
                SELECT q FROM Question q
                WHERE q.chapter.chapterId = :chapterId
                AND q.questionId NOT IN :practicedQuestionIds
                AND ( :level IS NULL OR :level = '' OR LOWER(q.difficultyLevel) = LOWER(:level) )
                AND (:usageType IS NULL OR :usageType = '' OR q.usageType = :usageType OR q.usageType = 'Both')
                ORDER BY q.questionId ASC
                LIMIT :limit
            """)
    List<Question> findUnpracticedQuestionsByChapter(String chapterId, List<Long> practicedQuestionIds, int limit,
            String level, String usageType);

    @Query(value = """
                SELECT * FROM questions
                WHERE chapter_id = :chapterId
                AND (:usageType IS NULL OR :usageType = '' OR usage_type = :usageType OR usage_type = 'Both')
                ORDER BY question_id ASC
                LIMIT :limit
            """, nativeQuery = true)
    List<Question> findByChapterIdWithLimit(@Param("chapterId") String chapterId, @Param("limit") int limit, @Param("usageType") String usageType);

    // Creator tracking methods
    List<Question> findByCreatedBy(User creator);

    List<Question> findByCreatedByIsNull();

    List<Question> findByCreatedByAndChapter_ChapterId(User creator, String chapterId);

    List<Question> findByCreatedByIsNullAndChapter_ChapterId(String chapterId);

    List<Question> findByIsCustom(Boolean isCustom);
}
