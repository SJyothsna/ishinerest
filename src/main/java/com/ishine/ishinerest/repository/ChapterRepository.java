package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, String> {
    List<Chapter> findBySubject_SubjectId(String subjectId);

    @Transactional
    @Modifying
    @Query("DELETE FROM Chapter c WHERE c.subject.subjectId = :subjectId")
    void deleteBySubjectId(String subjectId);
    boolean existsById(String chapterId);

}
