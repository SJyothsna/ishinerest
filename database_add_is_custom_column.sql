-- ============================================================================
-- Add IS_CUSTOM Column to Questions Table
-- ============================================================================
-- Hibernate couldn't create this column automatically because:
-- 1. The table already has data
-- 2. The column is defined as NOT NULL
-- 3. Hibernate doesn't know what default value to use for existing rows
-- ============================================================================

-- Add the IS_CUSTOM column with default value
ALTER TABLE questions 
ADD COLUMN is_custom BOOLEAN NOT NULL DEFAULT false;

-- Verify the column was added
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'QUESTIONS'
AND COLUMN_NAME = 'IS_CUSTOM';

-- Expected result:
-- IS_CUSTOM | BOOLEAN | NO | FALSE

-- ============================================================================
-- Now you can proceed with the data migration script
-- ============================================================================

-- Made with Bob
