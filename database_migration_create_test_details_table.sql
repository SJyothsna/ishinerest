-- Migration: Create test_details table
-- Purpose: Track per-question answers for assigned tests similar to practice_session_details
-- Date: 2026-05-10

CREATE TABLE IF NOT EXISTS test_details (
    test_detail_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NULL,
    question_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    student_answer TEXT NULL,
    is_correct BOOLEAN NOT NULL,
    attempt_count INT NOT NULL DEFAULT 1,

    CONSTRAINT fk_test_detail_assignment
        FOREIGN KEY (assignment_id) REFERENCES test_assignments(assignment_id) ON DELETE CASCADE,
    CONSTRAINT fk_test_detail_question
        FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE,
    CONSTRAINT fk_test_detail_student
        FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,

    INDEX idx_test_details_assignment_id (assignment_id),
    INDEX idx_test_details_question_id (question_id),
    INDEX idx_test_details_student_id (student_id)
);

-- Verification queries
SELECT 'test_details table created successfully' AS status;

DESCRIBE test_details;

SELECT
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
WHERE TABLE_NAME = 'test_details'
ORDER BY CONSTRAINT_TYPE, CONSTRAINT_NAME;

-- Made with Bob