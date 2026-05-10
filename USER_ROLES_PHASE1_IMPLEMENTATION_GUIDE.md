# User Roles System - Phase 1 Implementation Guide

## ✅ What We've Completed

### 1. Entity Classes Created
- ✅ `UserRole.java` - Enum for user roles (STUDENT, PARENT, TEACHER, ADMIN)
- ✅ `User.java` - Main user entity with role-based authentication
- ✅ `ParentStudentId.java` - Composite key for parent-student relationships
- ✅ `ParentStudent.java` - Parent-student relationship entity
- ✅ `TeacherStudentId.java` - Composite key for teacher-student relationships
- ✅ `TeacherStudent.java` - Teacher-student relationship entity

### 2. Existing Entities Updated
- ✅ `Student.java` - Added `user_id` foreign key to link with User table
- ✅ `Question.java` - Added creator tracking fields:
  - `createdBy` (User reference)
  - `isCustom` (boolean)
  - `visibility` (String)

### 3. Repository Interfaces Created
- ✅ `UserRepository.java` - CRUD operations for users
- ✅ `ParentStudentRepository.java` - Manage parent-student relationships
- ✅ `TeacherStudentRepository.java` - Manage teacher-student relationships

### 4. Migration Scripts Created
- ✅ `database_migration_user_roles_data_migration.sql` - Data migration script

---

## 🚀 Next Steps: Testing Phase 1

### Step 1: Restart Your Application

**What will happen:**
- Hibernate will detect the new entity classes
- It will automatically create new tables:
  - `users`
  - `parent_student`
  - `teacher_student`
- It will add new columns to existing tables:
  - `students.user_id`
  - `questions.created_by_user_id`
  - `questions.is_custom`
  - `questions.visibility`

**Action:**
```bash
# Stop your application if running
# Then start it again
```

**Expected Console Output:**
```sql
Hibernate: create table users (...)
Hibernate: create table parent_student (...)
Hibernate: create table teacher_student (...)
Hibernate: alter table students add column user_id bigint
Hibernate: alter table questions add column created_by_user_id bigint
Hibernate: alter table questions add column is_custom boolean not null
Hibernate: alter table questions add column visibility varchar(50)
```

### Step 2: Verify Tables Were Created

1. **Open H2 Console**: http://localhost:8080/h2-console
2. **Login**:
   - JDBC URL: `jdbc:h2:file:./data/testdb`
   - User: `SA`
   - Password: (empty)

3. **Run verification query**:
```sql
-- Check if new tables exist
SELECT TABLE_NAME 
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_NAME IN ('USERS', 'PARENT_STUDENT', 'TEACHER_STUDENT');

-- Check new columns in students table
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'STUDENTS' AND COLUMN_NAME = 'USER_ID';

-- Check new columns in questions table
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'QUESTIONS' 
AND COLUMN_NAME IN ('CREATED_BY_USER_ID', 'IS_CUSTOM', 'VISIBILITY');
```

**Expected Result:**
- All 3 new tables should exist
- New columns should be present in students and questions tables

### Step 3: Run Data Migration

**IMPORTANT:** Only run this AFTER Step 2 is successful!

1. **Still in H2 Console**, open the migration script:
   - File: `database_migration_user_roles_data_migration.sql`

2. **BEFORE running**, update the admin password hash:
   ```sql
   -- Line 43: Replace the dummy hash with a real BCrypt hash
   -- Generate one at: https://bcrypt-generator.com/
   -- Or use your existing admin password hash
   ```

3. **Copy and paste** the entire migration script into H2 Console

4. **Click "Run"**

5. **Verify migration** by running the verification queries at the bottom of the script

**Expected Results:**
```
-- Users by role
ROLE     | COUNT | ACTIVE_COUNT
---------|-------|-------------
STUDENT  | X     | X
ADMIN    | 1     | 1

-- Students linked to users
TOTAL_STUDENTS | STUDENTS_WITH_USER_LINK | STUDENTS_WITHOUT_USER_LINK
---------------|-------------------------|---------------------------
X              | X                       | 0

-- Questions with creators
TOTAL_QUESTIONS | QUESTIONS_WITH_CREATOR | CUSTOM_QUESTIONS | OFFICIAL_QUESTIONS
----------------|------------------------|------------------|-------------------
X               | X                      | 0                | X
```

### Step 4: Test the System

#### Test 1: Verify User Table
```sql
SELECT user_id, email, name, role, is_active 
FROM users 
ORDER BY role, user_id;
```

**Expected:** See all your students + 1 admin user

#### Test 2: Verify Student-User Links
```sql
SELECT s.student_id, s.name, s.email, u.user_id, u.role
FROM students s
LEFT JOIN users u ON s.user_id = u.user_id
LIMIT 10;
```

**Expected:** Every student should have a matching user record

#### Test 3: Verify Question Creators
```sql
SELECT q.question_id, q.question_text, q.is_custom, q.visibility, 
       u.name as creator_name, u.role as creator_role
FROM questions q
LEFT JOIN users u ON q.created_by_user_id = u.user_id
LIMIT 10;
```

**Expected:** All questions should be linked to admin user, is_custom=false

---

## 🎯 Success Criteria

Phase 1 is complete when:
- ✅ All new tables created successfully
- ✅ All new columns added to existing tables
- ✅ All existing students migrated to users table
- ✅ All students linked to their user records
- ✅ Admin user created
- ✅ All existing questions linked to admin user
- ✅ No errors in application startup
- ✅ Existing functionality still works (student login, questions, etc.)

---

## 🐛 Troubleshooting

### Issue: Tables not created
**Solution:** Check application.properties has `spring.jpa.hibernate.ddl-auto=update`

### Issue: Foreign key constraint errors
**Solution:** 
1. Stop application
2. Delete H2 database file: `data/testdb.mv.db`
3. Restart application (will recreate from scratch)
4. Run migration script again

### Issue: Duplicate key errors in migration
**Solution:** Some data already migrated. Check with:
```sql
SELECT COUNT(*) FROM users;
```
If users exist, skip migration or use the rollback script

### Issue: Admin user not created
**Solution:** Check if email already exists:
```sql
SELECT * FROM users WHERE email = 'admin@ishine.com';
```
If exists, update password:
```sql
UPDATE users 
SET password_hash = '$2a$10$YOUR_NEW_HASH'
WHERE email = 'admin@ishine.com';
```

---

## 📋 Next Phase Preview

After Phase 1 is complete and tested, Phase 2 will include:
- Update AuthService to support multi-role signup/login
- Create UserController for user management
- Create ParentController for parent-student linking
- Create TeacherController for teacher-student linking
- Update QuestionController for creator tracking
- Update admin panel UI

---

## 📝 Notes

- **Backup your database** before running migrations
- **Test in development** before production
- **Keep the rollback script** handy in case of issues
- **Document any custom changes** you make

---

## ✅ Checklist

- [ ] Application restarted successfully
- [ ] New tables created (users, parent_student, teacher_student)
- [ ] New columns added (user_id, created_by_user_id, is_custom, visibility)
- [ ] Data migration script executed
- [ ] All students migrated to users table
- [ ] All students linked to user records
- [ ] Admin user created with correct password
- [ ] All questions linked to admin
- [ ] Verification queries passed
- [ ] Existing student login still works
- [ ] Existing questions still display correctly
- [ ] No errors in application logs

---

**Ready to proceed? Start with Step 1: Restart Your Application!**