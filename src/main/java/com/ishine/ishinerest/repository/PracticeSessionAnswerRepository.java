package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.PracticeSession;
import com.ishine.ishinerest.entity.PracticeSessionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PracticeSessionAnswerRepository extends JpaRepository<PracticeSessionAnswer, Long> {

    List<PracticeSessionAnswer> findBySession_SessionIdOrderByAttemptOrderAsc(String sessionId);

    /** Graded (answered) attempts of a student in a chapter from submitted sessions, oldest first. */
    @Query("""
            SELECT a FROM PracticeSessionAnswer a
            JOIN FETCH a.session s
            WHERE s.studentId = :studentId
              AND s.chapterId = :chapterId
              AND s.status = :status
              AND a.isCorrect IS NOT NULL
            ORDER BY a.answeredAt ASC, a.answerId ASC
            """)
    List<PracticeSessionAnswer> findGradedAnswers(@Param("studentId") Long studentId,
            @Param("chapterId") String chapterId,
            @Param("status") PracticeSession.Status status);
}
