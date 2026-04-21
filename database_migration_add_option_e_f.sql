-- Migration script to add optionE and optionF columns to questions table
-- Run this script to support 6 answer options (A through F)

ALTER TABLE questions ADD COLUMN option_e VARCHAR(255);
ALTER TABLE questions ADD COLUMN option_f VARCHAR(255);

-- Verify the changes
SELECT column_name, data_type, character_maximum_length 
FROM information_schema.columns 
WHERE table_name = 'questions' 
AND column_name IN ('option_e', 'option_f');

-- Made with Bob
