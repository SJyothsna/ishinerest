package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.StudentNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentNoteRepository extends JpaRepository<StudentNote, Long> {

    List<StudentNote> findByStudentIdAndChapterIdOrderByUpdatedAtDesc(Long studentId, String chapterId);

    List<StudentNote> findByStudentIdOrderByUpdatedAtDesc(Long studentId);

    Page<StudentNote> findByStudentIdOrderByUpdatedAtDesc(Long studentId, Pageable pageable);

    Optional<StudentNote> findByIdAndStudentId(Long id, Long studentId);

    @Query("SELECT sn FROM StudentNote sn WHERE sn.studentId = :studentId " +
            "ORDER BY " +
            "CASE WHEN :sortBy = 'createdAt' THEN sn.createdAt END DESC, " +
            "CASE WHEN :sortBy = 'updatedAt' THEN sn.updatedAt END DESC, " +
            "CASE WHEN :sortBy = 'title' THEN sn.title END ASC")
    Page<StudentNote> findByStudentIdWithSorting(
            @Param("studentId") Long studentId,
            @Param("sortBy") String sortBy,
            Pageable pageable);

    long countByStudentId(Long studentId);
}

// Made with Bob
