-- ============================================================================
-- Database Migration: Create Feedback Table
-- Description: Creates the feedback table for storing contact form submissions
--              and user feedback with email notification support
-- Author: Bob
-- Date: 2024
-- ============================================================================

-- Create feedback table
CREATE TABLE IF NOT EXISTS feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL COMMENT 'Name of the person submitting feedback',
  email VARCHAR(255) NOT NULL COMMENT 'Email address for response',
  type ENUM('INQUIRY', 'FEEDBACK', 'SUPPORT', 'OTHER') NOT NULL DEFAULT 'INQUIRY' COMMENT 'Type of feedback submission',
  subject VARCHAR(500) NOT NULL COMMENT 'Subject line of the feedback',
  message TEXT NOT NULL COMMENT 'Detailed feedback message',
  status ENUM('NEW', 'READ', 'IN_PROGRESS', 'RESOLVED') NOT NULL DEFAULT 'NEW' COMMENT 'Processing status',
  user_id BIGINT NULL COMMENT 'Optional link to authenticated user',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Submission timestamp',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
  
  -- Indexes for performance
  INDEX idx_status (status),
  INDEX idx_type (type),
  INDEX idx_created_at (created_at),
  INDEX idx_email (email),
  
  -- Foreign key constraint
  CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) 
    REFERENCES users(user_id) 
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Stores user feedback and contact form submissions';

-- ============================================================================
-- Verification Queries
-- ============================================================================

-- Verify table structure
DESCRIBE feedback;

-- Check indexes
SHOW INDEX FROM feedback;

-- Check foreign key constraints
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_NAME = 'feedback'
  AND CONSTRAINT_SCHEMA = DATABASE()
  AND REFERENCED_TABLE_NAME IS NOT NULL;

-- ============================================================================
-- Sample Data (Optional - for testing)
-- ============================================================================

-- Insert sample feedback entries
INSERT INTO feedback (name, email, type, subject, message, status) VALUES
('John Doe', 'john.doe@example.com', 'INQUIRY', 'Question about subscription', 'I would like to know more about the premium features.', 'NEW'),
('Jane Smith', 'jane.smith@example.com', 'FEEDBACK', 'Great platform!', 'I love using iStudy for my studies. The practice questions are very helpful.', 'READ'),
('Mike Johnson', 'mike.j@example.com', 'SUPPORT', 'Login issue', 'I am having trouble logging into my account.', 'IN_PROGRESS'),
('Sarah Williams', 'sarah.w@example.com', 'OTHER', 'Partnership opportunity', 'I would like to discuss a potential partnership.', 'NEW');

-- Verify sample data
SELECT 
    id,
    name,
    email,
    type,
    subject,
    status,
    created_at
FROM feedback
ORDER BY created_at DESC;

-- ============================================================================
-- Rollback Script (if needed)
-- ============================================================================

-- To rollback this migration, uncomment and run:
-- DROP TABLE IF EXISTS feedback;

-- ============================================================================
-- Notes
-- ============================================================================
-- 1. The user_id field is optional (NULL) to allow non-authenticated submissions
-- 2. Email notifications are handled by the application layer (FeedbackService)
-- 3. The status field tracks the lifecycle of feedback processing
-- 4. Indexes are added for common query patterns (status, type, date, email)
-- 5. Foreign key constraint ensures referential integrity with users table
-- ============================================================================

-- Made with Bob