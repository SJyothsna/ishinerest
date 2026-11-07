package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.YoutubeVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface YoutubeVideoRepository extends JpaRepository<YoutubeVideo, Long> {

    @Query("SELECT v FROM YoutubeVideo v LEFT JOIN FETCH v.section s " +
           "WHERE v.chapter.chapterId = :chapterId " +
           "ORDER BY v.displayOrder ASC")
    List<YoutubeVideo> findByChapterId(String chapterId);

    @Query("SELECT v FROM YoutubeVideo v JOIN FETCH v.chapter c LEFT JOIN FETCH v.section s " +
           "WHERE v.subject.subjectId = :subjectId " +
           "ORDER BY c.chapterName ASC, v.displayOrder ASC")
    List<YoutubeVideo> findBySubjectId(String subjectId);

    @Query("SELECT v FROM YoutubeVideo v JOIN FETCH v.section s " +
           "WHERE v.section.sectionId = :sectionId " +
           "ORDER BY v.displayOrder ASC")
    List<YoutubeVideo> findBySectionId(String sectionId);
}
