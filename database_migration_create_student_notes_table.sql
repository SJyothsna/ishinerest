-- ============================================================================
-- Migration Script: Create student_notes table
-- ============================================================================
-- Date: 2026-04-10
-- Description: Creates the student_notes table for storing students' personal
--              notes for each chapter. Each student can create their own notes.
-- ============================================================================

-- IMPORTANT: 
-- 1. Backup your database before running this migration!
-- 2. Run this in H2 Console: http://localhost:8080/h2-console
--    JDBC URL: jdbc:h2:file:./data/testdb
--    Username: sa
--    Password: (leave empty)

-- ============================================================================
-- Step 1: Create student_notes table
-- ============================================================================
CREATE TABLE IF NOT EXISTS student_notes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    chapter_id VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    image_url VARCHAR(500),
    color VARCHAR(7) DEFAULT '#FFF9C4',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_student_notes_student 
        FOREIGN KEY (student_id) 
        REFERENCES students(student_id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_student_notes_chapter 
        FOREIGN KEY (chapter_id) 
        REFERENCES chapters(chapter_id) 
        ON DELETE CASCADE
);

-- ============================================================================
-- Step 2: Create indexes for better performance
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_student_notes_student_chapter ON student_notes(student_id, chapter_id);
CREATE INDEX IF NOT EXISTS idx_student_notes_student_id ON student_notes(student_id);
CREATE INDEX IF NOT EXISTS idx_student_notes_chapter_id ON student_notes(chapter_id);
CREATE INDEX IF NOT EXISTS idx_student_notes_updated_at ON student_notes(updated_at);

-- ============================================================================
-- Step 3: Insert sample data for testing
-- ============================================================================
-- Note: Replace student_id values with actual student IDs from your database
-- You can find student IDs by running: SELECT student_id, name FROM students;

INSERT INTO student_notes (student_id, chapter_id, title, content, image_url, color) VALUES
(1, 'CH001', 'My Study Notes', 'Remember to practice these problems:
1. Quadratic equations
2. Factoring
3. Completing the square', NULL, '#FFF9C4'),

(1, 'CH001', 'Important Formulas', 'x = (-b ± √(b² - 4ac)) / 2a

This is the quadratic formula. Use it when:
- The equation is in standard form
- Factoring is difficult', NULL, '#BBDEFB'),

(1, 'CH001', 'Exam Tips', 'For the upcoming exam:
✓ Review all formulas
✓ Practice word problems
✓ Check your work twice
✓ Manage your time', NULL, '#C8E6C9');

-- ============================================================================
-- Step 4: Verify the data
-- ============================================================================
-- SELECT sn.*, s.name as student_name, c.chapter_name 
-- FROM student_notes sn
-- JOIN students s ON sn.student_id = s.student_id
-- JOIN chapters c ON sn.chapter_id = c.chapter_id
-- ORDER BY sn.updated_at DESC;

-- ============================================================================
-- Useful Queries
-- ============================================================================

-- Get all notes for a specific student and chapter:
-- SELECT * FROM student_notes 
-- WHERE student_id = 1 AND chapter_id = 'CH001' 
-- ORDER BY updated_at DESC;

-- Get total notes count per student:
-- SELECT student_id, COUNT(*) as note_count 
-- FROM student_notes 
-- GROUP BY student_id;

-- Get most recent notes across all students:
-- SELECT sn.*, s.name as student_name 
-- FROM student_notes sn
-- JOIN students s ON sn.student_id = s.student_id
-- ORDER BY sn.updated_at DESC
-- LIMIT 10;

-- ============================================================================
-- Rollback script (if needed):
-- ============================================================================
-- DROP TABLE IF EXISTS student_notes;

-- Made with Bob