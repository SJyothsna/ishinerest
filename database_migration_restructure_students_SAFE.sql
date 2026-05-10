-- Safe Migration: Restructure students table to use user_id as primary key
-- This version properly handles foreign key constraints
-- Date: 2026-05-08

-- STEP 1: Drop foreign key constraints that reference students.student_id
-- Based on your error: FK8W8VDMSDW8O4R6ATRBVPBTH75, FKSWB7U87JFVKWJOWWT8CP1JG6F, FKI82V92PGN62R4WOPBBNUXKHKW

-- Drop foreign keys from flagged_questions
ALTER TABLE flagged_questions DROP CONSTRAINT IF EXISTS FK8W8VDMSDW8O4R6ATRBVPBTH75;
ALTER TABLE flagged_questions DROP CONSTRAINT IF EXISTS fk_flagged_questions_student;
ALTER TABLE flagged_questions DROP FOREIGN KEY IF EXISTS FK8W8VDMSDW8O4R6ATRBVPBTH75;

-- Drop foreign keys from practice_session_details
ALTER TABLE practice_session_details DROP CONSTRAINT IF EXISTS FKSWB7U87JFVKWJOWWT8CP1JG6F;
ALTER TABLE practice_session_details DROP CONSTRAINT IF EXISTS fk_practice_session_details_student;
ALTER TABLE practice_session_details DROP FOREIGN KEY IF EXISTS FKSWB7U87JFVKWJOWWT8CP1JG6F;

-- Drop foreign keys from student_notes
ALTER TABLE student_notes DROP CONSTRAINT IF EXISTS FKI82V92PGN62R4WOPBBNUXKHKW;
ALTER TABLE student_notes DROP CONSTRAINT IF EXISTS fk_student_notes_student;
ALTER TABLE student_notes DROP FOREIGN KEY IF EXISTS FKI82V92PGN62R4WOPBBNUXKHKW;

-- Drop any other foreign keys that might exist
ALTER TABLE student_subjects DROP CONSTRAINT IF EXISTS fk_student_subjects_student;
ALTER TABLE student_subjects DROP FOREIGN KEY IF EXISTS fk_student_subjects_student;

-- STEP 2: Create backup
CREATE TABLE students_backup AS SELECT * FROM students;

-- STEP 3: Create temporary mapping table to track old_id -> new_id
CREATE TABLE student_id_mapping (
    old_student_id BIGINT,
    new_student_id BIGINT,
    user_id BIGINT
);

INSERT INTO student_id_mapping (old_student_id, new_student_id, user_id)
SELECT student_id, user_id, user_id FROM students WHERE user_id IS NOT NULL;

-- STEP 4: Update foreign key references in other tables to use user_id
-- Update flagged_questions
UPDATE flagged_questions fq
JOIN student_id_mapping m ON fq.student_id = m.old_student_id
SET fq.student_id = m.new_student_id;

-- Update practice_session_details
UPDATE practice_session_details psd
JOIN student_id_mapping m ON psd.student_id = m.old_student_id
SET psd.student_id = m.new_student_id;

-- Update student_notes
UPDATE student_notes sn
JOIN student_id_mapping m ON sn.student_id = m.old_student_id
SET sn.student_id = m.new_student_id;

-- Update student_subjects if exists
UPDATE student_subjects ss
JOIN student_id_mapping m ON ss.student_id = m.old_student_id
SET ss.student_id = m.new_student_id
WHERE EXISTS (SELECT 1 FROM student_id_mapping);

-- STEP 5: Now we can safely drop the students table
DROP TABLE students;

-- STEP 6: Create new students table with correct structure
CREATE TABLE students (
    student_id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    class_id BIGINT NULL,
    CONSTRAINT fk_students_user FOREIGN KEY (student_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_students_class FOREIGN KEY (class_id) REFERENCES classes(class_id)
);

-- STEP 7: Insert data using user_id as student_id
INSERT INTO students (student_id, name, email, password_hash, class_id)
SELECT user_id, name, email, password_hash, class_id
FROM students_backup
WHERE user_id IS NOT NULL;

-- STEP 8: Recreate foreign keys in other tables
ALTER TABLE flagged_questions
ADD CONSTRAINT fk_flagged_questions_student 
FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE;

ALTER TABLE practice_session_details
ADD CONSTRAINT fk_practice_session_details_student 
FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE;

ALTER TABLE student_notes
ADD CONSTRAINT fk_student_notes_student 
FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE;

-- STEP 9: Clean up
DROP TABLE student_id_mapping;

-- STEP 10: Verification
SELECT 'Migration completed successfully!' AS status;

-- Verify structure
SELECT 
    COLUMN_NAME, 
    DATA_TYPE, 
    IS_NULLABLE,
    COLUMN_KEY
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'students' 
ORDER BY ORDINAL_POSITION;

-- Verify data: student_id should equal user_id
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

-- Count check
SELECT 
    (SELECT COUNT(*) FROM students) AS students_count,
    (SELECT COUNT(*) FROM users WHERE role = 'STUDENT') AS student_users_count,
    (SELECT COUNT(*) FROM students_backup) AS backup_count;

-- Made with Bob
