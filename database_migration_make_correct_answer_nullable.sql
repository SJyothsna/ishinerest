-- Migration script to make correct_answer column nullable in questions table
-- This allows Self Check questions to have no correct answer

-- For H2 Database (your current database)
ALTER TABLE questions ALTER COLUMN correct_answer VARCHAR(255) NULL;

-- For MySQL (if you switch to MySQL later)
-- ALTER TABLE questions MODIFY COLUMN correct_answer VARCHAR(255) NULL;

-- For PostgreSQL (if you switch to PostgreSQL later)
-- ALTER TABLE questions ALTER COLUMN correct_answer DROP NOT NULL;

-- Made with Bob