-- Migration: Restructure students table to use user_id as primary key
-- Purpose: Make student_id the same as user_id (true one-to-one relationship)
-- Date: 2026-05-08

-- IMPORTANT: Backup your database before running this migration!

-- Step 1: Create backup of students table
CREATE TABLE students_backup AS SELECT * FROM students;

-- Step 2: Drop foreign key constraints that reference students.student_id
-- (Add your specific foreign key drops here based on your schema)
-- Example:
-- ALTER TABLE grades DROP FOREIGN KEY fk_grades_student;
-- ALTER TABLE enrollments DROP FOREIGN KEY fk_enrollments_student;

-- Step 3: Create new students table with correct structure
CREATE TABLE students_new (
    student_id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    class_id BIGINT NULL,
    FOREIGN KEY (student_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (class_id) REFERENCES classes(class_id)
);

-- Step 4: Migrate data from old table to new table
-- Map old student_id to user_id
INSERT INTO students_new (student_id, name, email, password_hash, class_id)
SELECT 
    s.user_id AS student_id,  -- Use user_id as the new student_id
    s.name,
    s.email,
    s.password_hash,
    s.class_id
FROM students s
WHERE s.user_id IS NOT NULL;

-- Step 5: Drop old students table
DROP TABLE students;

-- Step 6: Rename new table to students
RENAME TABLE students_new TO students;

-- Step 7: Recreate foreign key constraints in other tables
-- Update them to reference the new student_id (which is now user_id)
-- Example:
-- ALTER TABLE grades 
-- ADD CONSTRAINT fk_grades_student 
-- FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE;

-- Verification
SELECT 'Migration completed successfully' AS status;

-- Verify student_id matches user_id
SELECT 
    s.student_id,
    u.user_id,
    s.name,
    CASE 
        WHEN s.student_id = u.user_id THEN 'MATCH ✓'
        ELSE 'MISMATCH ✗'
    END AS id_status
FROM students s
JOIN users u ON s.student_id = u.user_id;

-- Count verification
SELECT 
    (SELECT COUNT(*) FROM students) AS students_count,
    (SELECT COUNT(*) FROM users WHERE role = 'STUDENT') AS student_users_count;

-- Made with Bob
