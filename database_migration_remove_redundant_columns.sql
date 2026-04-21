-- Migration Script: Remove redundant chapterId and subjectId columns from practice_session_details
-- Date: 2026-02-07
-- Description: These columns are redundant as the data can be accessed through Question -> Chapter -> Subject relationships

-- IMPORTANT: Backup your database before running this migration!

-- Step 1: Verify the columns exist and check data
SELECT 
    COUNT(*) as total_records,
    COUNT(chapter_id) as records_with_chapter_id,
    COUNT(subject_id) as records_with_subject_id
FROM practice_session_details;

-- Step 2: Drop the redundant columns
-- Note: Uncomment the following lines when you're ready to execute the migration

-- ALTER TABLE practice_session_details DROP COLUMN chapter_id;
-- ALTER TABLE practice_session_details DROP COLUMN subject_id;

-- Step 3: Verify the columns are dropped
-- SELECT * FROM practice_session_details LIMIT 5;

-- Rollback script (if needed):
-- ALTER TABLE practice_session_details ADD COLUMN chapter_id VARCHAR(255);
-- ALTER TABLE practice_session_details ADD COLUMN subject_id VARCHAR(255);

-- Made with Bob
