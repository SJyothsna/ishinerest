-- SQL script to add exam-related columns to the classes table
-- and modify class_id column type from VARCHAR to INTEGER
-- Execute this script to update your database schema

-- IMPORTANT: Backup your data before running this script!

-- Step 1: Add new exam columns
ALTER TABLE classes ADD COLUMN exam VARCHAR(255);
ALTER TABLE classes ADD COLUMN exam_id INT;

-- Step 2: Modify the existing class_id column from VARCHAR to INTEGER
-- Note: This requires careful handling of existing data
-- Option A: If class_id contains only numeric values, you can convert directly
ALTER TABLE classes ALTER COLUMN class_id TYPE INTEGER USING class_id::INTEGER;

-- Option B: If you need to preserve data and have a backup, you might need to:
-- 1. Create a new column
-- ALTER TABLE classes ADD COLUMN class_id_new INTEGER;
-- 2. Copy and convert data
-- UPDATE classes SET class_id_new = CAST(class_id AS INTEGER);
-- 3. Drop old column and rename new one
-- ALTER TABLE classes DROP COLUMN class_id;
-- ALTER TABLE classes RENAME COLUMN class_id_new TO class_id;
-- 4. Add primary key constraint
-- ALTER TABLE classes ADD PRIMARY KEY (class_id);

-- Step 3: Optional - Add comments to document the columns
COMMENT ON COLUMN classes.exam IS 'Exam name or description';
COMMENT ON COLUMN classes.exam_id IS 'Numeric identifier for the exam';
COMMENT ON COLUMN classes.class_id IS 'Primary key - numeric identifier for the class (manually assigned)';

-- Step 4: Optional - Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_classes_exam_id ON classes(exam_id);

-- Made with Bob
