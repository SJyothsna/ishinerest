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
    @Query(value = """
                SELECT q.* FROM questions q
                JOIN chapters c ON q.chapter_id = c.chapter_id
                WHERE c.subject_id = :subjectId
                AND q.question_id NOT IN (:practicedQuestionIds)
                AND (:usageType IS NULL OR :usageType = ''
                     OR LOWER(COALESCE(q.usage_type, 'both')) = LOWER(:usageType)
                     OR LOWER(COALESCE(q.usage_type, 'both')) = 'both')
                ORDER BY
                    CASE WHEN LOWER(:usageType) = 'test' THEN RAND() END,
                    CASE WHEN :usageType IS NULL OR :usageType = '' OR LOWER(:usageType) <> 'test' THEN q.question_id END ASC
                LIMIT :limit
            """, nativeQuery = true)
    List<Question> findUnpracticedQuestionsBySubject(@Param("subjectId") String subjectId,
            @Param("practicedQuestionIds") List<Long> practicedQuestionIds,
            @Param("limit") int limit,
            @Param("usageType") String usageType);

    // Fetch questions by subject with limit (when no practiced questions)
    @Query(value = """
                SELECT q.* FROM questions q
                JOIN chapters c ON q.chapter_id = c.chapter_id
                WHERE c.subject_id = :subjectId
                AND (:usageType IS NULL OR :usageType = ''
                     OR LOWER(COALESCE(q.usage_type, 'both')) = LOWER(:usageType)
                     OR LOWER(COALESCE(q.usage_type, 'both')) = 'both')
                ORDER BY
                    CASE WHEN LOWER(:usageType) = 'test' THEN RAND() END,
                    CASE WHEN :usageType IS NULL OR :usageType = '' OR LOWER(:usageType) <> 'test' THEN q.question_id END ASC
                LIMIT :limit
            """, nativeQuery = true)
    List<Question> findBySubjectIdWithLimit(@Param("subjectId") String subjectId, @Param("limit") int limit, @Param("usageType") String usageType);

    // Fetch unpracticed questions by chapter
    @Query(value = """
                SELECT q.* FROM questions q
                WHERE q.chapter_id = :chapterId
                  AND q.question_id NOT IN (:practicedQuestionIds)
                  AND (:level IS NULL OR :level = '' OR LOWER(q.difficulty_level) = LOWER(:level))
                  AND (:usageType IS NULL OR :usageType = ''
                       OR LOWER(COALESCE(q.usage_type, 'both')) = LOWER(:usageType)
                       OR LOWER(COALESCE(q.usage_type, 'both')) = 'both')
                  AND (:sectionId IS NULL OR :sectionId = ''
                       OR LOWER(COALESCE(q.section_id, '')) = LOWER(:sectionId))
                ORDER BY q.question_id ASC
                LIMIT :limit
            """, nativeQuery = true)
    List<Question> findUnpracticedQuestionsByChapter(@Param("chapterId") String chapterId,
            @Param("practicedQuestionIds") List<Long> practicedQuestionIds,
            @Param("limit") int limit,
            @Param("level") String level,
            @Param("usageType") String usageType,
            @Param("sectionId") String sectionId);

    @Query(value = """
                SELECT * FROM questions
                WHERE chapter_id = :chapterId
                  AND (:usageType IS NULL OR :usageType = ''
                       OR LOWER(COALESCE(usage_type, 'both')) = LOWER(:usageType)
                       OR LOWER(COALESCE(usage_type, 'both')) = 'both')
                  AND (:sectionId IS NULL OR :sectionId = ''
                       OR LOWER(COALESCE(section_id, '')) = LOWER(:sectionId))
                ORDER BY question_id ASC
                LIMIT :limit
            """, nativeQuery = true)
    List<Question> findByChapterIdWithLimit(@Param("chapterId") String chapterId,
            @Param("limit") int limit,
            @Param("usageType") String usageType,
            @Param("sectionId") String sectionId);

    // Creator tracking methods
    List<Question> findByCreatedBy(User creator);

    List<Question> findByCreatedByIsNull();

    List<Question> findByCreatedByAndChapter_ChapterId(User creator, String chapterId);

    List<Question> findByCreatedByIsNullAndChapter_ChapterId(String chapterId);

    List<Question> findByIsCustom(Boolean isCustom);

    @Query("""
            SELECT DISTINCT q.sectionId
            FROM Question q
            WHERE q.chapter.chapterId = :chapterId
              AND q.sectionId IS NOT NULL
              AND TRIM(q.sectionId) <> ''
            ORDER BY q.sectionId
            """)
    List<String> findDistinctSectionIdsByChapterId(@Param("chapterId") String chapterId);
}
