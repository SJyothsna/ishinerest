package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.PrevExamQuestion;
import com.ishine.ishinerest.pojo.PrevExamQuestionResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PrevExamQuestionRepository extends JpaRepository<PrevExamQuestion, Long> {
    List<PrevExamQuestion> findByChapterId(String chapterId);

    @Query("""
    SELECT new com.ishine.ishinerest.pojo.PrevExamQuestionResponse(
        q.chapterId,
        p.examYear,
        p.paperType,
        q.section,
        q.qId,
        q.question,
        q.markingScheme
    )
    FROM PrevExamQuestion q
    JOIN PrevExamPaper p ON q.examId = p.examId
    WHERE q.chapterId = :chapterId
""")
    List<PrevExamQuestionResponse> findQuestionsByChapterId(String chapterId);

}

