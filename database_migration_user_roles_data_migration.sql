-- ============================================================================
-- User Roles System - Data Migration Script
-- ============================================================================
-- This script migrates existing data after Hibernate has created the new tables
-- Run this AFTER restarting the application (Hibernate will create tables first)
-- ============================================================================

-- Step 1: Migrate existing students to users table
-- ============================================================================
INSERT INTO users (email, name, password_hash, role, is_active, created_at, updated_at)
SELECT 
    email, 
    name, 
    password_hash, 
    'STUDENT' as role,
    true as is_active,
    CURRENT_TIMESTAMP as created_at,
    CURRENT_TIMESTAMP as updated_at
FROM students
WHERE email NOT IN (SELECT email FROM users);

-- Step 2: Link students to their user records
-- ============================================================================
UPDATE students s
SET user_id = (
    SELECT user_id 
    FROM users u 
    WHERE u.email = s.email 
    AND u.role = 'STUDENT'
    LIMIT 1
)
WHERE user_id IS NULL;

-- Step 3: Create admin user (change password hash to your actual admin password)
-- ============================================================================
-- Note: Generate a proper BCrypt hash for your admin password
-- You can use: https://bcrypt-generator.com/ or generate in your app
INSERT INTO users (email, name, password_hash, role, is_active, created_at, updated_at)
SELECT 
    'admin@ishine.com',
    'Admin',
    '$2a$10$dummyhashREPLACEWITHREALHASH',  -- REPLACE THIS WITH REAL BCRYPT HASH
    'ADMIN',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@ishine.com'
);

-- Step 4: Link existing questions to admin user
-- ============================================================================
UPDATE questions 
SET created_by_user_id = (
    SELECT user_id 
    FROM users 
    WHERE role = 'ADMIN' 
    LIMIT 1
),
is_custom = false,
visibility = 'PUBLIC'
WHERE created_by_user_id IS NULL;

-- ============================================================================
-- Verification Queries (Run these to verify the migration)
-- ============================================================================

-- Check users table
SELECT role, COUNT(*) as count, COUNT(CASE WHEN is_active THEN 1 END) as active_count
FROM users
GROUP BY role;

-- Check students linked to users
SELECT 
    COUNT(*) as total_students,
    COUNT(user_id) as students_with_user_link,
    COUNT(*) - COUNT(user_id) as students_without_user_link
FROM students;

-- Check questions linked to creators
SELECT 
    COUNT(*) as total_questions,
    COUNT(created_by_user_id) as questions_with_creator,
    COUNT(*) - COUNT(created_by_user_id) as questions_without_creator,
    COUNT(CASE WHEN is_custom = true THEN 1 END) as custom_questions,
    COUNT(CASE WHEN is_custom = false THEN 1 END) as official_questions
FROM questions;

-- Check admin user exists
SELECT user_id, email, name, role, is_active
FROM users
WHERE role = 'ADMIN';

-- ============================================================================
-- Rollback Script (Use if something goes wrong)
-- ============================================================================
-- CAUTION: This will undo all migrations. Use only if needed!
-- 
-- -- Remove user links from students
-- UPDATE students SET user_id = NULL;
-- 
-- -- Remove creator links from questions
-- UPDATE questions SET created_by_user_id = NULL, is_custom = false, visibility = 'PUBLIC';
-- 
-- -- Delete all users
-- DELETE FROM users;
-- 
-- ============================================================================

-- Made with Bob
