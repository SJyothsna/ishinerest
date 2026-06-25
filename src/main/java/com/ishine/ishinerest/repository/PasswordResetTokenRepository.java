package com.ishine.ishinerest.repository;

import com.ishine.ishinerest.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Repository for PasswordResetToken entity
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    /**
     * Find token by token string
     */
    Optional<PasswordResetToken> findByToken(String token);
    
    /**
     * Delete all tokens for a specific user
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken t WHERE t.userId = :userId")
    void deleteByUserId(Long userId);
    
    /**
     * Delete expired and used tokens (for cleanup)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < CURRENT_TIMESTAMP OR t.isUsed = true")
    void deleteExpiredAndUsedTokens();
}

// Made with Bob
