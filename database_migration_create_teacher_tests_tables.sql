CREATE TABLE IF NOT EXISTS teacher_tests (
    test_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    subject_id VARCHAR(100) NULL,
    chapter_id VARCHAR(100) NULL,
    duration_minutes INT NULL,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_teacher_tests_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS teacher_test_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    display_order INT NOT NULL,
    CONSTRAINT fk_teacher_test_questions_test
        FOREIGN KEY (test_id) REFERENCES teacher_tests(test_id) ON DELETE CASCADE,
    CONSTRAINT fk_teacher_test_questions_question
        FOREIGN KEY (question_id) REFERENCES questions(question_id),
    CONSTRAINT uq_teacher_test_question UNIQUE (test_id, question_id),
    CONSTRAINT uq_teacher_test_display_order UNIQUE (test_id, display_order)
);

-- Made with Bob
