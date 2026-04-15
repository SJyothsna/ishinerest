package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.SubjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<SubjectEntity, String> {
    @Query("SELECT s FROM SubjectEntity s JOIN s.classes c WHERE c.classId = :classId")
    List<SubjectEntity> findByClassId(@Param("classId") Integer classId);

    List<SubjectEntity> findBySubjectIdIn(List<String> subjectIds);
}
