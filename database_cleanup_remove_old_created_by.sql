-- ============================================================================
-- CLEANUP: Remove Old CREATED_BY Column from Questions Table
-- ============================================================================
-- The old CREATED_BY column (String) is no longer needed because we now have:
-- - CREATED_BY_USER_ID (foreign key to users table)
-- - Proper relationship through User entity
-- ============================================================================

-- Step 1: Check what data is in the old column (for verification)
SELECT DISTINCT created_by 
FROM questions 
WHERE created_by IS NOT NULL;

-- Expected: Should show old string values or NULL

-- Step 2: Remove the old CREATED_BY column
ALTER TABLE questions 
DROP COLUMN created_by;

-- Step 3: Verify the column is removed
SELECT COLUMN_NAME 
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'QUESTIONS'
AND COLUMN_NAME = 'CREATED_BY';

-- Expected: 0 rows (column no longer exists)

-- Step 4: Verify the new column still exists
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'QUESTIONS'
AND COLUMN_NAME = 'CREATED_BY_USER_ID';

-- Expected: CREATED_BY_USER_ID | BIGINT | YES

-- ============================================================================
-- RESULT: Questions table now has only CREATED_BY_USER_ID (proper foreign key)
-- ============================================================================

-- Made with Bob
