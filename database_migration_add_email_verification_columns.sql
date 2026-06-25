-- Migration: Add email verification columns to users table
-- This migration adds email verification functionality to the users table

-- Step 1: Add columns as nullable first
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN;
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_token VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_token_expiry TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMP;

-- Step 2: Set default value for existing users (mark them as verified)
UPDATE users SET email_verified = TRUE WHERE email_verified IS NULL;

-- Step 3: Make email_verified NOT NULL after setting defaults
ALTER TABLE users ALTER COLUMN email_verified SET NOT NULL;

-- Verification query
SELECT user_id, email, email_verified, email_verified_at FROM users;

-- Made with Bob
