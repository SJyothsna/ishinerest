-- ============================================
-- STUDENTS TABLE CLEANUP MIGRATION
-- Remove duplicate columns: name, email, password_hash
-- These columns duplicate data in the users table
-- ============================================
-- Author: Bob
-- Date: 2026-05-09
-- Database: MySQL/MariaDB
-- ============================================

-- IMPORTANT: BACKUP YOUR DATABASE BEFORE RUNNING THIS SCRIPT!

-- ============================================
-- STEP 1: PRE-MIGRATION VERIFICATION
-- ============================================

SELECT '=== STEP 1: PRE-MIGRATION VERIFICATION ===' AS step;

-- Check current students table structure
SELECT 'Current students table structure:' AS info;
DESCRIBE students;

-- Count total students
SELECT 
    'Total students in database:' AS info,
    COUNT(*) AS count
FROM students;

-- ============================================
-- STEP 2: DATA CONSISTENCY CHECK
-- ============================================

SELECT '=== STEP 2: DATA CONSISTENCY CHECK ===' AS step;

-- Verify that student data matches user data
SELECT 
    'Data Consistency Summary:' AS info,
    COUNT(*) AS total_students,
    SUM(CASE WHEN s.name = u.name THEN 1 ELSE 0 END) AS name_matches,
    SUM(CASE WHEN s.email = u.email THEN 1 ELSE 0 END) AS email_matches,
    SUM(CASE WHEN s.password_hash = u.password_hash THEN 1 ELSE 0 END) AS password_matches,
    SUM(CASE WHEN s.name = u.name AND s.email = u.email AND s.password_hash = u.password_hash THEN 1 ELSE 0 END) AS all_match
FROM students s
JOIN users u ON s.student_id = u.user_id;

-- Show any inconsistencies (if any exist)
SELECT 
    'INCONSISTENT DATA (if any):' AS warning,
    s.student_id,
    s.name AS student_name,
    u.name AS user_name,
    s.email AS student_email,
    u.email AS user_email,
    CASE 
        WHEN s.name != u.name THEN 'NAME_MISMATCH'
        WHEN s.email != u.email THEN 'EMAIL_MISMATCH'
        WHEN s.password_hash != u.password_hash THEN 'PASSWORD_MISMATCH'
    END AS issue_type
FROM students s
JOIN users u ON s.student_id = u.user_id
WHERE s.name != u.name 
   OR s.email != u.email 
   OR s.password_hash != u.password_hash;

-- If inconsistencies found, you may want to resolve them before proceeding
-- Example: UPDATE students s JOIN users u ON s.student_id = u.user_id SET s.name = u.name WHERE s.name != u.name;

-- ============================================
-- STEP 3: CREATE BACKUP TABLE
-- ============================================

SELECT '=== STEP 3: CREATE BACKUP TABLE ===' AS step;

-- Drop backup table if it exists from previous run
DROP TABLE IF EXISTS students_backup_duplicate_cleanup;

-- Create backup of students table
CREATE TABLE students_backup_duplicate_cleanup AS 
SELECT * FROM students;

-- Verify backup
SELECT 
    'Backup created successfully:' AS info,
    COUNT(*) AS backup_row_count
FROM students_backup_duplicate_cleanup;

-- ============================================
-- STEP 4: REMOVE DUPLICATE COLUMNS
-- ============================================

SELECT '=== STEP 4: REMOVE DUPLICATE COLUMNS ===' AS step;

-- Remove name column
ALTER TABLE students DROP COLUMN name;
SELECT 'Dropped column: name' AS status;

-- Remove email column
ALTER TABLE students DROP COLUMN email;
SELECT 'Dropped column: email' AS status;

-- Remove password_hash column
ALTER TABLE students DROP COLUMN password_hash;
SELECT 'Dropped column: password_hash' AS status;

-- ============================================
-- STEP 5: POST-MIGRATION VERIFICATION
-- ============================================

SELECT '=== STEP 5: POST-MIGRATION VERIFICATION ===' AS step;

-- Check new students table structure
SELECT 'New students table structure:' AS info;
DESCRIBE students;

-- Verify row count unchanged
SELECT 
    'Row count verification:' AS info,
    (SELECT COUNT(*) FROM students) AS current_count,
    (SELECT COUNT(*) FROM students_backup_duplicate_cleanup) AS backup_count,
    CASE 
        WHEN (SELECT COUNT(*) FROM students) = (SELECT COUNT(*) FROM students_backup_duplicate_cleanup)
        THEN 'PASS - Row counts match'
        ELSE 'FAIL - Row counts do not match!'
    END AS status;

-- Verify all students still have valid user references
SELECT 
    'User reference integrity:' AS info,
    COUNT(*) AS students_with_valid_user_ref
FROM students s
JOIN users u ON s.student_id = u.user_id;

-- Test join with users table (should work seamlessly)
SELECT 
    'Sample data after migration (first 5 students):' AS info;
    
SELECT 
    s.student_id,
    u.name,
    u.email,
    u.role,
    s.class_id
FROM students s
JOIN users u ON s.student_id = u.user_id
LIMIT 5;

-- ============================================
-- STEP 6: VERIFY FOREIGN KEY CONSTRAINTS
-- ============================================

SELECT '=== STEP 6: VERIFY FOREIGN KEY CONSTRAINTS ===' AS step;

-- Check if foreign key exists
SELECT 
    'Foreign key constraints on students table:' AS info,
    CONSTRAINT_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'students'
  AND REFERENCED_TABLE_NAME IS NOT NULL;

-- ============================================
-- STEP 7: FINAL SUMMARY
-- ============================================

SELECT '=== STEP 7: MIGRATION SUMMARY ===' AS step;

SELECT 
    'Migration completed successfully!' AS status,
    'Columns removed: name, email, password_hash' AS changes,
    'Backup table: students_backup_duplicate_cleanup' AS backup_info,
    'Next steps: Test application, then drop backup table' AS next_action;

-- ============================================
-- CLEANUP (RUN AFTER TESTING)
-- ============================================

-- IMPORTANT: Only run this after thoroughly testing the application!
-- Uncomment the line below to drop the backup table:

-- DROP TABLE students_backup_duplicate_cleanup;

-- ============================================
-- ROLLBACK PROCEDURE (IF NEEDED)
-- ============================================

/*
If you need to rollback this migration:

1. Stop the application

2. Restore the students table:
   DROP TABLE students;
   RENAME TABLE students_backup_duplicate_cleanup TO students;

3. Verify restoration:
   DESCRIBE students;
   SELECT COUNT(*) FROM students;

4. Revert code changes (git revert)

5. Restart application
*/

-- ============================================
-- END OF MIGRATION SCRIPT
-- ============================================

-- Made with Bob
