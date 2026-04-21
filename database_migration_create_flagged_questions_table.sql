-- ============================================================================
-- Migration Script: Create flagged_questions table
-- ============================================================================
-- Date: 2026-04-20
-- Description: Creates the flagged_questions table for storing questions that
--              students have flagged for review. Each student can flag questions
--              they want to revisit later.
-- ============================================================================

-- IMPORTANT: 
-- 1. Backup your database before running this migration!
-- 2. Run this in H2 Console: http://localhost:8080/h2-console
--    JDBC URL: jdbc:h2:file:./data/testdb
--    Username: sa
--    Password: (leave empty)

-- ============================================================================
-- Step 1: Create flagged_questions table
-- ============================================================================
CREATE TABLE IF NOT EXISTS flagged_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    flagged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_flagged_questions_student 
        FOREIGN KEY (student_id) 
        REFERENCES students(student_id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_flagged_questions_question 
        FOREIGN KEY (question_id) 
        REFERENCES questions(question_id) 
        ON DELETE CASCADE,
    
    -- Ensure a student can only flag a question once
    CONSTRAINT uk_student_question UNIQUE (student_id, question_id)
);

-- ============================================================================
-- Step 2: Create indexes for better performance
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_flagged_questions_student_id ON flagged_questions(student_id);
CREATE INDEX IF NOT EXISTS idx_flagged_questions_question_id ON flagged_questions(question_id);
CREATE INDEX IF NOT EXISTS idx_flagged_questions_flagged_at ON flagged_questions(flagged_at);

-- ============================================================================
-- Step 3: Insert sample data for testing (optional)
-- ============================================================================
-- Note: Replace student_id and question_id values with actual IDs from your database
-- You can find student IDs by running: SELECT student_id, name FROM students;
-- You can find question IDs by running: SELECT question_id, question_text FROM questions LIMIT 10;

-- Example: Student 4 flags some questions
-- INSERT INTO flagged_questions (student_id, question_id) VALUES
-- (4, 204),
-- (4, 205),
-- (4, 210);

-- ============================================================================
-- Step 4: Verify the data
-- ============================================================================
-- SELECT fq.*, s.name as student_name, q.question_text 
-- FROM flagged_questions fq
-- JOIN students s ON fq.student_id = s.student_id
-- JOIN questions q ON fq.question_id = q.question_id
-- ORDER BY fq.flagged_at DESC;

-- ============================================================================
-- Useful Queries
-- ============================================================================

-- Get all flagged questions for a specific student:
-- SELECT fq.*, q.question_text, q.question_type, q.difficulty_level
-- FROM flagged_questions fq
-- JOIN questions q ON fq.question_id = q.question_id
-- WHERE fq.student_id = 4
-- ORDER BY fq.flagged_at DESC;

-- Get all flagged questions for a student in a specific chapter:
-- SELECT fq.*, q.question_text, c.chapter_name
-- FROM flagged_questions fq
-- JOIN questions q ON fq.question_id = q.question_id
-- JOIN chapters c ON q.chapter_id = c.chapter_id
-- WHERE fq.student_id = 4 AND c.chapter_id = 'LC5H0102'
-- ORDER BY fq.flagged_at DESC;

-- Get count of flagged questions per student:
-- SELECT student_id, COUNT(*) as flagged_count 
-- FROM flagged_questions 
-- GROUP BY student_id;

-- Get most recently flagged questions across all students:
-- SELECT fq.*, s.name as student_name, q.question_text
-- FROM flagged_questions fq
-- JOIN students s ON fq.student_id = s.student_id
-- JOIN questions q ON fq.question_id = q.question_id
-- ORDER BY fq.flagged_at DESC
-- LIMIT 10;

-- ============================================================================
-- Rollback script (if needed):
-- ============================================================================
-- DROP TABLE IF EXISTS flagged_questions;

-- Made with Bob