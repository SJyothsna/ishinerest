package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.PracticeSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PracticeSessionRepository extends JpaRepository<PracticeSession, String> {
}
