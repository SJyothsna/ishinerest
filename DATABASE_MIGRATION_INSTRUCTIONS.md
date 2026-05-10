# Database Migration Instructions

## Problem
The application is throwing errors: `Value too long for column "NOTES CHARACTER VARYING(255)"` because the database columns are too small for the content being stored.

## Solution
Run the SQL migration scripts to:
1. Add the `usage_type` column
2. Change text columns from VARCHAR(255) to TEXT (unlimited size)

## Step-by-Step Instructions

### Option 1: Using H2 Console (Recommended - Easy)

1. **Keep your Spring Boot application running** (it's already running on port 8080)

2. **Open H2 Console in your browser:**
   ```
   http://localhost:8080/h2-console
   ```

3. **Login with these credentials:**
   - JDBC URL: `jdbc:h2:file:./data/testdb`
   - User Name: `sa`
   - Password: (leave empty)
   - Click "Connect"

4. **Run the first migration (add usage_type column):**
   - Copy and paste this SQL into the console:
   ```sql
   ALTER TABLE questions ADD COLUMN usage_type VARCHAR(50);
   ```
   - Click "Run" button
   - You should see: "Update count: 0" (success)

5. **Run the second migration (fix text column sizes):**
   - Copy and paste this SQL into the console:
   ```sql
   ALTER TABLE questions ALTER COLUMN question_text TEXT;
   ALTER TABLE questions ALTER COLUMN explanation TEXT;
   ALTER TABLE questions ALTER COLUMN notes TEXT;
   ```
   - Click "Run" button
   - You should see: "Update count: 0" (success)

6. **Verify the changes:**
   - Run this query to check the table structure:
   ```sql
   SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
   WHERE TABLE_NAME = 'QUESTIONS' 
   AND COLUMN_NAME IN ('USAGE_TYPE', 'NOTES', 'EXPLANATION', 'QUESTION_TEXT');
   ```
   - You should see all four columns with TEXT or VARCHAR types

7. **Test the application:**
   - Go back to your admin page: `http://localhost:8080/admin.html`
   - Try editing a question with long notes/explanation
   - It should now work without errors!

### Option 2: Using SQL Files (Alternative)

If you prefer to run the SQL files directly:

1. **Stop your Spring Boot application** (if running)

2. **Run migrations using H2 command line:**
   ```powershell
   # Navigate to your project directory
   cd c:/Users/JyothsnaSirasanameti/projects/learning/ishinerest
   
   # Run the migrations (you'll need H2 jar file)
   java -cp "C:/Users/JyothsnaSirasanameti/.m2/repository/com/h2database/h2/2.2.224/h2-2.2.224.jar" org.h2.tools.RunScript -url "jdbc:h2:file:./data/testdb" -user sa -script database_migration_add_usage_type_to_questions.sql
   
   java -cp "C:/Users/JyothsnaSirasanameti/.m2/repository/com/h2database/h2/2.2.224/h2-2.2.224.jar" org.h2.tools.RunScript -url "jdbc:h2:file:./data/testdb" -user sa -script database_migration_increase_text_columns_size.sql
   ```

3. **Restart your Spring Boot application**

## What Changed

### 1. Added `usage_type` Column
- **Column:** `usage_type VARCHAR(50)`
- **Purpose:** Categorize questions as "Practice", "Test", or "Both"
- **Default:** NULL (will be set to "Both" for existing questions via application logic)

### 2. Changed Text Columns to TEXT Type
- **Columns changed:**
  - `question_text`: VARCHAR(255) → TEXT
  - `explanation`: VARCHAR(255) → TEXT
  - `notes`: VARCHAR(255) → TEXT
- **Purpose:** Allow unlimited text length (no more 255 character limit)
- **Impact:** Existing data is preserved, just the column type changes

## Verification

After running migrations, verify everything works:

1. **Check the admin page loads:** `http://localhost:8080/admin.html`
2. **Test adding a question** with the new Usage Type field
3. **Test editing a question** with long notes/explanation (>255 characters)
4. **Test filtering questions** by Usage Type
5. **Check the API endpoints** work with the new field

## Troubleshooting

### If you get "Column already exists" error:
- The `usage_type` column was already added by Hibernate
- Skip the first migration, only run the second one (text column sizes)

### If you still get "Value too long" errors:
- Make sure you ran the second migration (text column sizes)
- Restart your Spring Boot application after running migrations
- Check the H2 console to verify column types changed to TEXT

### If H2 Console won't connect:
- Make sure your Spring Boot application is running
- Check the JDBC URL matches: `jdbc:h2:file:./data/testdb`
- Username should be `sa` with no password

## Files Created

1. `database_migration_add_usage_type_to_questions.sql` - Adds usage_type column
2. `database_migration_increase_text_columns_size.sql` - Changes VARCHAR to TEXT
3. `DATABASE_MIGRATION_INSTRUCTIONS.md` - This file (instructions)

## Next Steps After Migration

Once migrations are complete:
1. Test the admin interface thoroughly
2. Test API endpoints with the new `usageType` parameter
3. Update existing questions to set appropriate usage_type values
4. Consider setting a default value for usage_type in future questions

---
**Created by Bob - Your AI Assistant**