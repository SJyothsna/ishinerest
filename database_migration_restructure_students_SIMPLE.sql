-- Simple Migration for H2: Restructure students table
-- Uses delete/recreate approach to avoid foreign key issues
-- Date: 2026-05-08

-- STEP 1: Create backup of all tables
CREATE TABLE students_backup AS SELECT * FROM students;
CREATE TABLE flagged_questions_backup AS SELECT * FROM flagged_questions;
CREATE TABLE practice_session_details_backup AS SELECT * FROM practice_session_details;
CREATE TABLE student_notes_backup AS SELECT * FROM student_notes;

-- STEP 2: Drop all foreign key constraints
ALTER TABLE flagged_questions DROP CONSTRAINT IF EXISTS FK8W8VDMSDW8O4R6ATRBVPBTH75;
ALTER TABLE flagged_questions DROP CONSTRAINT IF EXISTS fk_flagged_questions_student;
ALTER TABLE practice_session_details DROP CONSTRAINT IF EXISTS FKSWB7U87JFVKWJOWWT8CP1JG6F;
ALTER TABLE practice_session_details DROP CONSTRAINT IF EXISTS fk_practice_session_details_student;
ALTER TABLE student_notes DROP CONSTRAINT IF EXISTS FKI82V92PGN62R4WOPBBNUXKHKW;
ALTER TABLE student_notes DROP CONSTRAINT IF EXISTS fk_student_notes_student;

-- STEP 3: Delete all data from dependent tables (we have backups!)
DELETE FROM flagged_questions;
DELETE FROM practice_session_details;
DELETE FROM student_notes;

-- STEP 4: Drop and recreate students table
DROP TABLE students;

CREATE TABLE students (
    student_id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    class_id BIGINT NULL,
    CONSTRAINT fk_students_user FOREIGN KEY (student_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_students_class FOREIGN KEY (class_id) REFERENCES classes(class_id)
);

-- STEP 5: Insert students data with user_id as student_id
INSERT INTO students (student_id, name, email, password_hash, class_id)
SELECT user_id, name, email, password_hash, class_id
FROM students_backup
WHERE user_id IS NOT NULL;

-- STEP 6: Recreate foreign keys on dependent tables
ALTER TABLE flagged_questions
ADD CONSTRAINT fk_flagged_questions_student 
FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE;

ALTER TABLE practice_session_details
ADD CONSTRAINT fk_practice_session_details_student 
FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE;

ALTER TABLE student_notes
ADD CONSTRAINT fk_student_notes_student 
FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE;

-- STEP 7: Restore data to dependent tables with updated student_ids
-- Restore flagged_questions
INSERT INTO flagged_questions (id, student_id, question_id, flagged_at)
SELECT 
    fq.id,
    sb.user_id AS student_id,  -- Use user_id as new student_id
    fq.question_id,
    fq.flagged_at
FROM flagged_questions_backup fq
JOIN students_backup sb ON fq.student_id = sb.student_id
WHERE sb.user_id IS NOT NULL;

-- Restore practice_session_details
INSERT INTO practice_session_details (session_detail_id, question_id, student_answer, is_correct, student_id, attempt_count)
SELECT 
    psd.session_detail_id,
    psd.question_id,
    psd.student_answer,
    psd.is_correct,
    sb.user_id AS student_id,  -- Use user_id as new student_id
    psd.attempt_count
FROM practice_session_details_backup psd
JOIN students_backup sb ON psd.student_id = sb.student_id
WHERE sb.user_id IS NOT NULL;

-- Restore student_notes
INSERT INTO student_notes (id, student_id, chapter_id, title, content, image_url, color, created_at, updated_at)
SELECT 
    sn.id,
    sb.user_id AS student_id,  -- Use user_id as new student_id
    sn.chapter_id,
    sn.title,
    sn.content,
    sn.image_url,
    sn.color,
    sn.created_at,
    sn.updated_at
FROM student_notes_backup sn
JOIN students_backup sb ON sn.student_id = sb.student_id
WHERE sb.user_id IS NOT NULL;

-- STEP 8: Verification
SELECT '✓ Migration completed successfully!' AS status;

-- Show new table structure (should NOT have user_id column)
SELECT COLUMN_NAME, TYPE_NAME, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'STUDENTS' 
ORDER BY ORDINAL_POSITION;

-- Verify student_id equals user_id
SELECT 
    s.student_id,
    u.user_id,
    s.name,
    s.email,
    CASE 
        WHEN s.student_id = u.user_id THEN '✓ MATCH'
        ELSE '✗ MISMATCH'
    END AS verification
FROM students s
JOIN users u ON s.student_id = u.user_id
LIMIT 10;

-- Count verification
SELECT 
    (SELECT COUNT(*) FROM students) AS students_count,
    (SELECT COUNT(*) FROM students_backup) AS backup_count,
    (SELECT COUNT(*) FROM flagged_questions) AS flagged_questions_count,
    (SELECT COUNT(*) FROM flagged_questions_backup) AS flagged_backup_count,
    (SELECT COUNT(*) FROM practice_session_details) AS practice_count,
    (SELECT COUNT(*) FROM practice_session_details_backup) AS practice_backup_count,
    (SELECT COUNT(*) FROM student_notes) AS notes_count,
    (SELECT COUNT(*) FROM student_notes_backup) AS notes_backup_count;

-- Show sample data
SELECT student_id, name, email FROM students LIMIT 5;

-- STEP 9: Optional - Drop backup tables after verification
-- Uncomment these lines after verifying everything works:
-- DROP TABLE students_backup;
-- DROP TABLE flagged_questions_backup;
-- DROP TABLE practice_session_details_backup;
-- DROP TABLE student_notes_backup;

SELECT 'Backup tables kept for safety. Drop them manually after verification.' AS note;

-- Made with Bob
