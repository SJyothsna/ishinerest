package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.PrevExamPaper;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrevExamPaperRepository extends JpaRepository<PrevExamPaper, Integer> {
    List<PrevExamPaper> findByLevel(String level);
}

