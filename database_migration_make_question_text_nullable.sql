-- Migration: Make question_text column nullable in questions table
-- This allows questions to have only an image without requiring text
-- Date: 2026-05-05
-- Description: When a question has an image (question_image_url), the question_text field should be optional

-- Make question_text nullable
ALTER TABLE questions MODIFY COLUMN question_text TEXT NULL;

-- Verification query to check the change
-- Run this after migration to verify:
-- DESCRIBE questions;
-- Look for question_text column and verify it shows NULL: YES

-- Made with Bob
