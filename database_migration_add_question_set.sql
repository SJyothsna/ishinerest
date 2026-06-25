-- ============================================================================
-- Database Migration: Add Question Set Column to Questions Table
-- ============================================================================
-- Purpose: Add question_set column to organize questions into sets (Set 1, 2, 3, etc.)
--          for progressive practice. Students can move to next set after completing current set.
-- 
-- Author: Bob
-- Date: 2026-05-22
-- ============================================================================

-- Step 1: Add question_set column to questions table
-- Using VARCHAR(20) to support formats like "1", "2", "Set A", etc.
-- Default value is "1" for backward compatibility
ALTER TABLE questions 
ADD COLUMN question_set VARCHAR(20) DEFAULT '1';

-- Step 2: Update existing questions to Set 1
-- This ensures all existing questions are assigned to Set 1
UPDATE questions 
SET question_set = '1' 
WHERE question_set IS NULL OR question_set = '';

-- Step 3: Add index for better query performance
-- This improves filtering performance when querying by question_set
CREATE INDEX idx_questions_question_set ON questions(question_set);

-- Step 4: Verify the migration
-- Uncomment the following lines to verify the changes
-- SELECT COUNT(*) as total_questions, question_set 
-- FROM questions 
-- GROUP BY question_set 
-- ORDER BY question_set;

-- ============================================================================
-- Rollback Instructions (if needed)
-- ============================================================================
-- To rollback this migration, execute:
-- DROP INDEX idx_questions_question_set;
-- ALTER TABLE questions DROP COLUMN question_set;
-- ============================================================================

-- Migration completed successfully

-- Made with Bob
