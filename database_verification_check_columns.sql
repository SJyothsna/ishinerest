-- ============================================================================
-- VERIFICATION SCRIPT: Check if Hibernate Created New Columns
-- ============================================================================
-- Run this BEFORE running the data migration script
-- This will help verify that Hibernate has created all necessary columns
-- ============================================================================

-- Check if new columns exist in questions table
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'QUESTIONS'
AND COLUMN_NAME IN ('CREATED_BY_USER_ID', 'IS_CUSTOM', 'VISIBILITY')
ORDER BY COLUMN_NAME;

-- Expected result: Should show 3 rows
-- CREATED_BY_USER_ID | BIGINT | YES
-- IS_CUSTOM | BOOLEAN | NO
-- VISIBILITY | VARCHAR | YES

-- Check if users table exists
SELECT COUNT(*) AS users_table_exists
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_NAME = 'USERS';

-- Expected result: 1 (table exists)

-- Check if parent_student table exists
SELECT COUNT(*) AS parent_student_table_exists
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_NAME = 'PARENT_STUDENT';

-- Expected result: 1 (table exists)

-- Check if teacher_student table exists
SELECT COUNT(*) AS teacher_student_table_exists
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_NAME = 'TEACHER_STUDENT';

-- Expected result: 1 (table exists)

-- Check if user_id column exists in students table
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'STUDENTS'
AND COLUMN_NAME = 'USER_ID';

-- Expected result: Should show 1 row
-- USER_ID | BIGINT | YES

-- ============================================================================
-- INTERPRETATION:
-- ============================================================================
-- If ANY of the above queries return 0 rows or empty results:
--   1. STOP - Do not run the data migration yet
--   2. Restart your Spring Boot application
--   3. Hibernate will automatically create the missing tables/columns
--   4. Run this verification script again
--   5. Once all checks pass, proceed with data migration
-- ============================================================================

-- Made with Bob
