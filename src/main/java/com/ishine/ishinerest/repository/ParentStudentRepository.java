package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.ParentStudent;
import com.ishine.ishinerest.entity.ParentStudent.LinkStatus;
import com.ishine.ishinerest.entity.ParentStudentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ParentStudent relationship entity
 */
@Repository
public interface ParentStudentRepository extends JpaRepository<ParentStudent, ParentStudentId> {
    
    List<ParentStudent> findByParent_UserId(Long parentId);
    
    List<ParentStudent> findByStudent_UserId(Long studentId);
    
    List<ParentStudent> findByParent_UserIdAndStatus(Long parentId, LinkStatus status);
    
    List<ParentStudent> findByStudent_UserIdAndStatus(Long studentId, LinkStatus status);
    
    boolean existsByParent_UserIdAndStudent_UserId(Long parentId, Long studentId);
    
    boolean existsByParent_UserIdAndStudent_UserIdAndStatus(Long parentId, Long studentId, LinkStatus status);
    
    Optional<ParentStudent> findByParent_UserIdAndStudent_UserId(Long parentId, Long studentId);
}

// Made with Bob
