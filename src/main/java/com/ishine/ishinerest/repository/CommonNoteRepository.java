package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.CommonNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommonNoteRepository extends JpaRepository<CommonNote, Long> {

    List<CommonNote> findByChapterIdAndIsActiveTrueOrderByDisplayOrderAsc(String chapterId);

    List<CommonNote> findByChapterIdOrderByDisplayOrderAsc(String chapterId);

    @Query("SELECT DISTINCT c.category FROM CommonNote c WHERE c.isActive = true ORDER BY c.category")
    List<String> findAllDistinctCategories();
}

// Made with Bob
