-- Complete Migration for H2: Restructure students table
-- Drops ALL constraints properly before restructuring
-- Date: 2026-05-08

-- STEP 1: Create backups
CREATE TABLE students_backup AS SELECT * FROM students;
CREATE TABLE flagged_questions_backup AS SELECT * FROM flagged_questions;
CREATE TABLE practice_session_details_backup AS SELECT * FROM practice_session_details;
CREATE TABLE student_notes_backup AS SELECT * FROM student_notes;

SELECT 'Backups created' AS status;

-- STEP 2: Drop ALL foreign key constraints that reference students
-- Drop from flagged_questions
ALTER TABLE flagged_questions DROP CONSTRAINT IF EXISTS FK8W8VDMSDW8O4R6ATRBVPBTH75;
ALTER TABLE flagged_questions DROP CONSTRAINT IF EXISTS fk_flagged_questions_student;
ALTER TABLE flagged_questions DROP CONSTRAINT IF EXISTS CONSTRAINT_8;
ALTER TABLE flagged_questions DROP CONSTRAINT IF EXISTS CONSTRAINT_82;

-- Drop from practice_session_details
ALTER TABLE practice_session_details DROP CONSTRAINT IF EXISTS FKSWB7U87JFVKWJOWWT8CP1JG6F;
ALTER TABLE practice_session_details DROP CONSTRAINT IF EXISTS fk_practice_session_details_student;
ALTER TABLE practice_session_details DROP CONSTRAINT IF EXISTS CONSTRAINT_F;
ALTER TABLE practice_session_details DROP CONSTRAINT IF EXISTS CONSTRAINT_F8;

-- Drop from student_notes
ALTER TABLE student_notes DROP CONSTRAINT IF EXISTS FKI82V92PGN62R4WOPBBNUXKHKW;
ALTER TABLE student_notes DROP CONSTRAINT IF EXISTS fk_student_notes_student;
ALTER TABLE student_notes DROP CONSTRAINT IF EXISTS CONSTRAINT_5;
ALTER TABLE student_notes DROP CONSTRAINT IF EXISTS CONSTRAINT_59;

SELECT 'Foreign keys dropped' AS status;

-- STEP 3: Drop the dependent tables completely (we have backups!)
DROP TABLE IF EXISTS flagged_questions;
DROP TABLE IF EXISTS practice_session_details;
DROP TABLE IF EXISTS student_notes;

SELECT 'Dependent tables dropped' AS status;

-- STEP 4: Now drop students table (no dependencies!)
DROP TABLE students;

SELECT 'Students table dropped' AS status;

-- STEP 5: Create new students table with correct structure
CREATE TABLE students (
    student_id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    class_id BIGINT NULL,
    CONSTRAINT fk_students_user FOREIGN KEY (student_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_students_class FOREIGN KEY (class_id) REFERENCES classes(class_id)
);

SELECT 'New students table created' AS status;

-- STEP 6: Insert students data with user_id as student_id
INSERT INTO students (student_id, name, email, password_hash, class_id)
SELECT user_id, name, email, password_hash, class_id
FROM students_backup
WHERE user_id IS NOT NULL;

SELECT 'Students data inserted' AS status;

-- STEP 7: Recreate flagged_questions table
CREATE TABLE flagged_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    flagged_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_flagged_questions_student FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    CONSTRAINT fk_flagged_questions_question FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE,
    CONSTRAINT uk_flagged_questions UNIQUE (student_id, question_id)
);

-- Restore flagged_questions data
INSERT INTO flagged_questions (id, student_id, question_id, flagged_at)
SELECT 
    fq.id,
    sb.user_id AS student_id,
    fq.question_id,
    fq.flagged_at
FROM flagged_questions_backup fq
JOIN students_backup sb ON fq.student_id = sb.student_id
WHERE sb.user_id IS NOT NULL;

SELECT 'Flagged questions restored' AS status;

-- STEP 8: Recreate practice_session_details table
CREATE TABLE practice_session_details (
    session_detail_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    student_answer VARCHAR(255),
    is_correct BOOLEAN NOT NULL,
    student_id BIGINT NOT NULL,
    attempt_count INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_practice_session_details_question FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE,
    CONSTRAINT fk_practice_session_details_student FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE
);

-- Restore practice_session_details data
INSERT INTO practice_session_details (session_detail_id, question_id, student_answer, is_correct, student_id, attempt_count)
SELECT 
    psd.session_detail_id,
    psd.question_id,
    psd.student_answer,
    psd.is_correct,
    sb.user_id AS student_id,
    psd.attempt_count
FROM practice_session_details_backup psd
JOIN students_backup sb ON psd.student_id = sb.student_id
WHERE sb.user_id IS NOT NULL;

SELECT 'Practice session details restored' AS status;

-- STEP 9: Recreate student_notes table
CREATE TABLE student_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    chapter_id VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    image_url VARCHAR(500),
    color VARCHAR(7) DEFAULT '#FFF9C4',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_student_notes_student FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
    CONSTRAINT fk_student_notes_chapter FOREIGN KEY (chapter_id) REFERENCES chapters(chapter_id) ON DELETE CASCADE
);

-- Restore student_notes data
INSERT INTO student_notes (id, student_id, chapter_id, title, content, image_url, color, created_at, updated_at)
SELECT 
    sn.id,
    sb.user_id AS student_id,
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

SELECT 'Student notes restored' AS status;

-- STEP 10: Verification
SELECT '✓✓✓ Migration completed successfully! ✓✓✓' AS status;

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
    (SELECT COUNT(*) FROM flagged_questions) AS flagged_count,
    (SELECT COUNT(*) FROM flagged_questions_backup) AS flagged_backup,
    (SELECT COUNT(*) FROM practice_session_details) AS practice_count,
    (SELECT COUNT(*) FROM practice_session_details_backup) AS practice_backup,
    (SELECT COUNT(*) FROM student_notes) AS notes_count,
    (SELECT COUNT(*) FROM student_notes_backup) AS notes_backup;

-- Show sample data
SELECT student_id, name, email FROM students LIMIT 5;

SELECT 'Backup tables kept for safety. Drop them manually after verification:' AS note;
SELECT 'DROP TABLE students_backup;' AS cleanup_sql;
SELECT 'DROP TABLE flagged_questions_backup;' AS cleanup_sql;
SELECT 'DROP TABLE practice_session_details_backup;' AS cleanup_sql;
SELECT 'DROP TABLE student_notes_backup;' AS cleanup_sql;

-- Made with Bob
