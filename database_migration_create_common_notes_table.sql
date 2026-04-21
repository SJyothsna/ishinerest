-- ============================================================================
-- Migration Script: Create common_notes table
-- ============================================================================
-- Date: 2026-04-10
-- Description: Creates the common_notes table for storing admin/teacher notes
--              for each chapter. These notes are shared with all students.
-- ============================================================================

-- IMPORTANT: 
-- 1. Backup your database before running this migration!
-- 2. Run this in H2 Console: http://localhost:8080/h2-console
--    JDBC URL: jdbc:h2:file:./data/testdb
--    Username: sa
--    Password: (leave empty)

-- ============================================================================
-- Step 1: Create common_notes table
-- ============================================================================
CREATE TABLE IF NOT EXISTS common_notes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chapter_id VARCHAR(50) NOT NULL,
    category VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    image_url VARCHAR(500),
    color VARCHAR(7) DEFAULT '#BBDEFB',
    icon VARCHAR(10) DEFAULT '📚',
    display_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    
    CONSTRAINT fk_common_notes_chapter 
        FOREIGN KEY (chapter_id) 
        REFERENCES chapters(chapter_id) 
        ON DELETE CASCADE
);

-- ============================================================================
-- Step 2: Create indexes for better performance
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_common_notes_chapter_id ON common_notes(chapter_id);
CREATE INDEX IF NOT EXISTS idx_common_notes_category ON common_notes(category);
CREATE INDEX IF NOT EXISTS idx_common_notes_active ON common_notes(is_active);

-- ============================================================================
-- Step 3: Insert sample data for testing
-- ============================================================================
INSERT INTO common_notes (chapter_id, category, title, content, image_url, color, icon, display_order) VALUES
('CH001', 'Formulas', 'Quadratic Formula', 'x = (-b ± √(b² - 4ac)) / 2a

Used to solve quadratic equations of the form ax² + bx + c = 0', NULL, '#BBDEFB', '📐', 1),

('CH001', 'Formulas', 'Pythagorean Theorem', 'a² + b² = c²

In a right triangle, the square of the hypotenuse equals the sum of squares of the other two sides.', NULL, '#BBDEFB', '📐', 2),

('CH001', 'Definitions', 'Prime Number', 'A prime number is a natural number greater than 1 that has no positive divisors other than 1 and itself.

Examples: 2, 3, 5, 7, 11, 13...', NULL, '#C8E6C9', '📚', 3),

('CH001', 'Definitions', 'Function', 'A function is a relation between a set of inputs and a set of outputs where each input is related to exactly one output.

Notation: f(x) = y', NULL, '#C8E6C9', '📚', 4),

('CH001', 'Tips & Tricks', 'Solving Word Problems', '1. Read the problem carefully
2. Identify what you need to find
3. List the given information
4. Choose a strategy
5. Solve and check your answer', NULL, '#FFE0B2', '💡', 5);

-- ============================================================================
-- Step 4: Verify the data
-- ============================================================================
-- SELECT * FROM common_notes ORDER BY display_order;

-- ============================================================================
-- Rollback script (if needed):
-- ============================================================================
-- DROP TABLE IF EXISTS common_notes;

-- Made with Bob