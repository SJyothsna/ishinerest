package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.PrevExamPaper;
import com.ishine.ishinerest.pojo.PrevExamPaperResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PrevExamPaperRepository extends JpaRepository<PrevExamPaper, String> {
        @Query("""
    SELECT new com.ishine.ishinerest.pojo.PrevExamPaperResponse(
        p.subjectId,
        p.examYear,
        p.paperType,
        p.title,
        p.questionPaper,
        p.markingScheme
    )
    FROM PrevExamPaper p
    WHERE p.subjectId = :subjectId
""")
        List<PrevExamPaperResponse> findPaperDetailsBySubjectId(String subjectId);
}
