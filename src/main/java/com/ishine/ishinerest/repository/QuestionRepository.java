package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.Question;

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
    """)
    List<Question> findUnpracticedQuestionsBySubject(String subjectId, List<Long> practicedQuestionIds);
    // Fetch unpracticed questions by chapter
    @Query("""
                SELECT q FROM Question q 
                WHERE q.chapter.chapterId = :chapterId 
                AND q.questionId NOT IN :practicedQuestionIds
                    ORDER BY q.questionId ASC    
                LIMIT :limit
            """)
    List<Question> findUnpracticedQuestionsByChapter(String chapterId, List<Long> practicedQuestionIds, int limit);

    @Query(value = """
    SELECT * FROM questions 
    WHERE chapter_id = :chapterId 
    ORDER BY question_id ASC
    LIMIT :limit
""", nativeQuery = true)
    List<Question> findByChapterIdWithLimit(@Param("chapterId") String chapterId, @Param("limit") int limit);

}
