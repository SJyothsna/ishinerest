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
                AND (:questionSet IS NULL OR :questionSet = ''
                     OR LOWER(COALESCE(q.question_set, '1')) = LOWER(:questionSet))
                ORDER BY
                    CASE WHEN LOWER(:usageType) = 'test' THEN RAND() END,
                    CASE WHEN :usageType IS NULL OR :usageType = '' OR LOWER(:usageType) <> 'test' THEN q.question_id END ASC
                LIMIT :limit
            """, nativeQuery = true)
    List<Question> findUnpracticedQuestionsBySubject(@Param("subjectId") String subjectId,
            @Param("practicedQuestionIds") List<Long> practicedQuestionIds,
            @Param("limit") int limit,
            @Param("usageType") String usageType,
            @Param("questionSet") String questionSet);

    // Fetch questions by subject with limit (when no practiced questions)
    @Query(value = """
                SELECT q.* FROM questions q
                JOIN chapters c ON q.chapter_id = c.chapter_id
                WHERE c.subject_id = :subjectId
                AND (:usageType IS NULL OR :usageType = ''
                     OR LOWER(COALESCE(q.usage_type, 'both')) = LOWER(:usageType)
                     OR LOWER(COALESCE(q.usage_type, 'both')) = 'both')
                AND (:questionSet IS NULL OR :questionSet = ''
                     OR LOWER(COALESCE(q.question_set, '1')) = LOWER(:questionSet))
                ORDER BY
                    CASE WHEN LOWER(:usageType) = 'test' THEN RAND() END,
                    CASE WHEN :usageType IS NULL OR :usageType = '' OR LOWER(:usageType) <> 'test' THEN q.question_id END ASC
                LIMIT :limit
            """, nativeQuery = true)
    List<Question> findBySubjectIdWithLimit(@Param("subjectId") String subjectId, @Param("limit") int limit, @Param("usageType") String usageType, @Param("questionSet") String questionSet);

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
                  AND (:questionSet IS NULL OR :questionSet = ''
                       OR LOWER(COALESCE(q.question_set, '1')) = LOWER(:questionSet))
                ORDER BY q.question_id ASC
                LIMIT :limit
            """, nativeQuery = true)
    List<Question> findUnpracticedQuestionsByChapter(@Param("chapterId") String chapterId,
            @Param("practicedQuestionIds") List<Long> practicedQuestionIds,
            @Param("limit") int limit,
            @Param("level") String level,
            @Param("usageType") String usageType,
            @Param("sectionId") String sectionId,
            @Param("questionSet") String questionSet);

    @Query(value = """
                SELECT * FROM questions
                WHERE chapter_id = :chapterId
                  AND (:usageType IS NULL OR :usageType = ''
                       OR LOWER(COALESCE(usage_type, 'both')) = LOWER(:usageType)
                       OR LOWER(COALESCE(usage_type, 'both')) = 'both')
                  AND (:sectionId IS NULL OR :sectionId = ''
                       OR LOWER(COALESCE(section_id, '')) = LOWER(:sectionId))
                  AND (:questionSet IS NULL OR :questionSet = ''
                       OR LOWER(COALESCE(question_set, '1')) = LOWER(:questionSet))
                ORDER BY question_id ASC
                LIMIT :limit
            """, nativeQuery = true)
    List<Question> findByChapterIdWithLimit(@Param("chapterId") String chapterId,
            @Param("limit") int limit,
            @Param("usageType") String usageType,
            @Param("sectionId") String sectionId,
            @Param("questionSet") String questionSet);

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

    /**
     * Finds questions similar to a source question using tag overlap.
     * Matches questions in the same chapter and difficulty that share at least one
     * comma-separated tag with the source. Excludes the source question and any
     * question the student has already answered correctly.
     *
     * H2 does not support FIND_IN_SET, so we use LOCATE / INSTR compatible syntax
     * that works on both H2 and MySQL: we look for each tag as a substring of the
     * tags column. This is intentionally loose (good for small tag sets).
     */
    @Query(value = """
            SELECT q.* FROM questions q
            WHERE q.chapter_id       = :chapterId
              AND q.question_id     != :sourceQuestionId
              AND q.difficulty_level = :difficultyLevel
              AND q.question_id NOT IN (
                  SELECT psd.question_id
                  FROM practice_session_details psd
                  WHERE psd.student_id = :studentId
                    AND psd.is_correct = true
              )
              AND (
                  LOCATE(:tag1, COALESCE(q.tags,'')) > 0
                  OR LOCATE(:tag2, COALESCE(q.tags,'')) > 0
                  OR LOCATE(:tag3, COALESCE(q.tags,'')) > 0
              )
            ORDER BY q.question_id ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Question> findSimilarQuestions(@Param("chapterId") String chapterId,
                                        @Param("sourceQuestionId") Long sourceQuestionId,
                                        @Param("difficultyLevel") String difficultyLevel,
                                        @Param("studentId") Long studentId,
                                        @Param("tag1") String tag1,
                                        @Param("tag2") String tag2,
                                        @Param("tag3") String tag3,
                                        @Param("limit") int limit);
}
