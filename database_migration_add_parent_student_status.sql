-- Migration: Add status and timestamp fields to parent_student table
-- Purpose: Enable pending/approval workflow for parent-student links
-- Date: 2026-05-08

-- Add status column with default PENDING
ALTER TABLE parent_student 
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

-- Add updated_at timestamp
ALTER TABLE parent_student 
ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Add approved_at timestamp (nullable)
ALTER TABLE parent_student 
ADD COLUMN approved_at TIMESTAMP NULL;

-- Add rejected_at timestamp (nullable)
ALTER TABLE parent_student 
ADD COLUMN rejected_at TIMESTAMP NULL;

-- Update existing records to ACTIVE status (backward compatibility)
UPDATE parent_student 
SET status = 'ACTIVE', 
    approved_at = created_at 
WHERE status = 'PENDING';

-- Add index for filtering by status
CREATE INDEX idx_parent_student_status ON parent_student(status);

-- Add composite index for common queries
CREATE INDEX idx_parent_student_lookup ON parent_student(student_user_id, status);

-- Verify migration
SELECT 'Migration completed successfully' AS status;
SELECT COUNT(*) AS total_links, status, COUNT(*) AS count_by_status 
FROM parent_student 
GROUP BY status;

-- Made with Bob
