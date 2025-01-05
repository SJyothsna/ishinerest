package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.Question;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByChapter_ChapterId(Long chapterId);

    @Query("SELECT q FROM Question q WHERE q.chapter.subject.subjectId = :subjectId")
    List<Question> findBySubjectId(@Param("subjectId") Long subjectId);


    // Fetch unpracticed questions by subject
    @Query("""
        SELECT q FROM Question q 
        WHERE q.chapter.subject.subjectId = :subjectId 
        AND q.questionId NOT IN :practicedQuestionIds
    """)
    List<Question> findUnpracticedQuestionsBySubject(Long subjectId, List<Long> practicedQuestionIds);
    // Fetch unpracticed questions by chapter
    @Query("""
        SELECT q FROM Question q 
        WHERE q.chapter.chapterId = :chapterId 
        AND q.questionId NOT IN :practicedQuestionIds
    """)
    List<Question> findUnpracticedQuestionsByChapter(Long chapterId, List<Long> practicedQuestionIds);

}
