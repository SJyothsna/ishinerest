package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.TeacherSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TeacherSubjectRepository extends JpaRepository<TeacherSubject, Long> {
    List<TeacherSubject> findByTeacherUserId(Long teacherUserId);

    boolean existsByTeacherUserIdAndSubjectId(Long teacherUserId, String subjectId);

    long countByTeacherUserId(Long teacherUserId);

    @Transactional
    @Modifying
    void deleteByTeacherUserId(Long teacherUserId);

    @Transactional
    @Modifying
    void deleteByTeacherUserIdAndSubjectId(Long teacherUserId, String subjectId);
}

// Made with Bob
