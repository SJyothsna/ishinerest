-- Migration script to increase size of text columns in questions table
-- Change from VARCHAR(255) to TEXT to support larger content
-- This fixes the "Value too long for column" error

-- For H2 Database (your current database)
ALTER TABLE questions ALTER COLUMN question_text TEXT;
ALTER TABLE questions ALTER COLUMN explanation TEXT;
ALTER TABLE questions ALTER COLUMN notes TEXT;

-- For MySQL (if you switch to MySQL later)
-- ALTER TABLE questions MODIFY COLUMN question_text TEXT;
-- ALTER TABLE questions MODIFY COLUMN explanation TEXT;
-- ALTER TABLE questions MODIFY COLUMN notes TEXT;

-- For PostgreSQL (if you switch to PostgreSQL later)
-- ALTER TABLE questions ALTER COLUMN question_text TYPE TEXT;
-- ALTER TABLE questions ALTER COLUMN explanation TYPE TEXT;
-- ALTER TABLE questions ALTER COLUMN notes TYPE TEXT;

-- Made with Bob
