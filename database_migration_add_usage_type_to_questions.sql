-- Migration script to add usage_type column to questions table
-- Possible values: 'Practice', 'Test', 'Both'

-- Add the usage_type column
ALTER TABLE questions ADD COLUMN usage_type VARCHAR(20);

-- Set default value to 'Both' for existing records
UPDATE questions SET usage_type = 'Both' WHERE usage_type IS NULL;

-- Add a check constraint to ensure only valid values are allowed
ALTER TABLE questions ADD CONSTRAINT chk_usage_type 
    CHECK (usage_type IN ('Practice', 'Test', 'Both'));

-- Add comment to the column
COMMENT ON COLUMN questions.usage_type IS 'Indicates where the question can be used: Practice, Test, or Both';

-- Made with Bob
