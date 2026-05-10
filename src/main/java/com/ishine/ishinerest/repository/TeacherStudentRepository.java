package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.TeacherStudent;
import com.ishine.ishinerest.entity.TeacherStudentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TeacherStudent relationship entity
 */
@Repository
public interface TeacherStudentRepository extends JpaRepository<TeacherStudent, TeacherStudentId> {
    
    List<TeacherStudent> findByTeacher_UserId(Long teacherId);
    
    List<TeacherStudent> findByStudent_UserId(Long studentId);
    
    boolean existsByTeacher_UserIdAndStudent_UserId(Long teacherId, Long studentId);
    
    List<TeacherStudent> findByTeacher_UserIdAndSubjectId(Long teacherId, String subjectId);
}

// Made with Bob
