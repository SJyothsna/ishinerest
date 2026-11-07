-- Migration: add attempted_at to practice_session_details
-- Run on MySQL (prod). H2 dev is handled by Hibernate ddl-auto=update.

-- Step 1: add as nullable so existing rows are not rejected
ALTER TABLE practice_session_details
    ADD COLUMN attempted_at DATETIME NULL;

-- Step 2: backfill existing rows
UPDATE practice_session_details SET attempted_at = NOW() WHERE attempted_at IS NULL;

-- Step 3: index for streak and recent-activity queries
CREATE INDEX idx_psd_student_attempted ON practice_session_details (student_id, attempted_at);
