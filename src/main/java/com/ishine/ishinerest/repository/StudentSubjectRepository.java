package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.StudentSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface StudentSubjectRepository extends JpaRepository<StudentSubject, Long> {
    List<StudentSubject> findByStudentId(Long studentId);

    boolean existsByStudentIdAndSubjectId(Long studentId, String subjectId);

    long countByStudentId(Long studentId);

    @Transactional
    @Modifying
    void deleteByStudentId(Long studentId);

}
