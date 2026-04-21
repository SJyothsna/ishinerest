-- ============================================================================
-- Migration Script: Remove CLASS_ID column from subjects table
-- ============================================================================
-- Date: 2026-04-10
-- Issue: Subjects table has CLASS_ID column with NOT NULL constraint
-- Solution: Remove CLASS_ID column as subjects use Many-to-Many relationship
--           with classes through the class_subjects join table
-- ============================================================================

-- IMPORTANT: 
-- 1. Backup your database before running this migration!
-- 2. Stop your Spring Boot application before running this script
-- 3. Run this in H2 Console: http://localhost:8080/h2-console
--    JDBC URL: jdbc:h2:file:./data/testdb
--    Username: sa
--    Password: (leave empty)

-- ============================================================================
-- Step 1: Check current schema (optional - for verification)
-- ============================================================================
-- Uncomment to see current structure:
-- SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'SUBJECTS';

-- ============================================================================
-- Step 2: Remove the CLASS_ID column from subjects table
-- ============================================================================
ALTER TABLE SUBJECTS DROP COLUMN IF EXISTS CLASS_ID;

-- ============================================================================
-- Step 3: Verify the change (optional)
-- ============================================================================
-- Uncomment to verify:
-- SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'SUBJECTS';
-- Should only show: SUBJECT_ID, SUBJECT_NAME

-- ============================================================================
-- Step 4: Test by viewing subjects data
-- ============================================================================
-- SELECT * FROM SUBJECTS;

-- ============================================================================
-- After running this migration:
-- 1. Restart your Spring Boot application
-- 2. Try creating a subject from the admin UI again
-- 3. Subjects will be linked to classes through the class_subjects table
-- ============================================================================

-- Rollback script (if needed):
-- ALTER TABLE SUBJECTS ADD COLUMN CLASS_ID INTEGER;
-- Note: Manual data population would be required if rolling back

-- Made with Bob