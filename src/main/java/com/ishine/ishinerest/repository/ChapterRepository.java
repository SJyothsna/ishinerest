package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, String> {
    List<Chapter> findBySubject_SubjectId(String subjectId);

    @Transactional
    @Modifying
    @Query("DELETE FROM Chapter c WHERE c.subject.subjectId = :subjectId")
    void deleteBySubjectId(String subjectId);
    boolean existsById(String chapterId);

    /**
     * Finds the next chapter in the same subject after the given chapterId.
     * "Next" is defined by the natural sort order of chapter_id (lexicographic),
     * which matches the existing manual-string chapterId convention.
     */
    @Query(value = """
            SELECT c.* FROM chapters c
            WHERE c.subject_id = (
                SELECT subject_id FROM chapters WHERE chapter_id = :currentChapterId
            )
            AND c.chapter_id > :currentChapterId
            ORDER BY c.chapter_id ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Chapter> findNextChapterInSubject(@Param("currentChapterId") String currentChapterId);

}
