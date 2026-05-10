-- Migration: Create test_assignments table
-- Purpose: Track test assignments to students by teachers/parents
-- Date: 2026-05-09

-- Create test_assignments table
CREATE TABLE IF NOT EXISTS test_assignments (
    assignment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_id BIGINT NOT NULL,
    student_user_id BIGINT NOT NULL,
    assigned_by_user_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_date TIMESTAMP NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    score INT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',
    feedback TEXT NULL,
    
    -- Foreign keys
    CONSTRAINT fk_test_assignment_test 
        FOREIGN KEY (test_id) REFERENCES teacher_tests(test_id) ON DELETE CASCADE,
    CONSTRAINT fk_test_assignment_student 
        FOREIGN KEY (student_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_test_assignment_assigned_by 
        FOREIGN KEY (assigned_by_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    -- Indexes for performance
    INDEX idx_test_assignments_test_id (test_id),
    INDEX idx_test_assignments_student_user_id (student_user_id),
    INDEX idx_test_assignments_assigned_by (assigned_by_user_id),
    INDEX idx_test_assignments_status (status),
    INDEX idx_test_assignments_due_date (due_date),
    
    -- Unique constraint: prevent duplicate assignments
    UNIQUE KEY uk_test_student (test_id, student_user_id)
);

-- Add check constraint for status values
ALTER TABLE test_assignments 
ADD CONSTRAINT chk_test_assignment_status 
CHECK (status IN ('ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'OVERDUE'));

-- Add check constraint for score range (0-100)
ALTER TABLE test_assignments 
ADD CONSTRAINT chk_test_assignment_score 
CHECK (score IS NULL OR (score >= 0 AND score <= 100));

-- Verification queries
SELECT 'test_assignments table created successfully' AS status;

-- Check table structure
DESCRIBE test_assignments;

-- Check constraints
SELECT 
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
WHERE TABLE_NAME = 'test_assignments'
ORDER BY CONSTRAINT_TYPE, CONSTRAINT_NAME;

-- Made with Bob