-- Migration script to add question_image_url column to questions table
-- This allows storing image URLs for questions

-- For H2 Database (your current database)
ALTER TABLE questions ADD COLUMN question_image_url VARCHAR(500);

-- For MySQL (if you switch to MySQL later)
-- ALTER TABLE questions ADD COLUMN question_image_url VARCHAR(500);

-- For PostgreSQL (if you switch to PostgreSQL later)
-- ALTER TABLE questions ADD COLUMN question_image_url VARCHAR(500);

-- Made with Bob