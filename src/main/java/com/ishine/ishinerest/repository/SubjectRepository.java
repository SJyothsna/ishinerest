package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.SubjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<SubjectEntity, String> {
    List<SubjectEntity> findByClassEntity_ClassId(String classId);
}
