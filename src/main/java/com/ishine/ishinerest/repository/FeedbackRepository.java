package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    // Find feedback by status
    List<Feedback> findByStatusOrderByCreatedAtDesc(Feedback.FeedbackStatus status);

    // Find feedback by type
    List<Feedback> findByTypeOrderByCreatedAtDesc(Feedback.FeedbackType type);

    // Find feedback by email
    List<Feedback> findByEmailOrderByCreatedAtDesc(String email);

    // Find feedback by user ID (for authenticated users)
    List<Feedback> findByUser_UserIdOrderByCreatedAtDesc(Long userId);
}

// Made with Bob