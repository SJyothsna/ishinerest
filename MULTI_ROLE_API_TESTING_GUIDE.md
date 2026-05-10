# Multi-Role User System - API Testing Guide

## 📋 Overview

This guide provides step-by-step instructions for testing all the new multi-role user system APIs.

## 🚀 Prerequisites

1. **Application Running**: Start your Spring Boot application
2. **Database Migrated**: Ensure Phase 1 database migration is complete
3. **REST Client**: Use VS Code REST Client extension or Postman

## 📁 Test Files Location

All test files are located in `src/test/java/rest/`:

- `multirole/multirole-auth.http` - Authentication tests
- `users/users.http` - User management tests
- `parents/parents.http` - Parent-student relationship tests
- `teachers/teachers.http` - Teacher-student relationship tests
- `customquestions/customquestions.http` - Custom question tests

## 🧪 Testing Workflow

### Phase 1: Authentication Testing

**File:** `multirole/multirole-auth.http`

#### 1.1 Test Multi-Role Signup

```http
# Test 1: Signup as STUDENT
POST http://localhost:8080/auth/signup
{
  "name": "John Student",
  "email": "john.student@example.com",
  "password": "password123",
  "role": "STUDENT"
}

Expected Response:
{
  "userId": 4,
  "name": "John Student",
  "email": "john.student@example.com",
  "role": "STUDENT",
  "studentId": 3  // Student record created
}
```

```http
# Test 2: Signup as PARENT
POST http://localhost:8080/auth/signup
{
  "name": "Jane Parent",
  "email": "jane.parent@example.com",
  "password": "password123",
  "role": "PARENT"
}

Expected Response:
{
  "userId": 5,
  "name": "Jane Parent",
  "email": "jane.parent@example.com",
  "role": "PARENT",
  "studentId": null  // No student record for parents
}
```

```http
# Test 3: Signup as TEACHER
POST http://localhost:8080/auth/signup
{
  "name": "Bob Teacher",
  "email": "bob.teacher@example.com",
  "password": "password123",
  "role": "TEACHER"
}

Expected Response:
{
  "userId": 6,
  "name": "Bob Teacher",
  "email": "bob.teacher@example.com",
  "role": "TEACHER",
  "studentId": null
}
```

```http
# Test 4: Try to signup as ADMIN (should fail)
POST http://localhost:8080/auth/signup
{
  "name": "Admin User",
  "email": "admin.user@example.com",
  "password": "password123",
  "role": "ADMIN"
}

Expected: 403 Forbidden
Error: "Admin accounts cannot be created through signup"
```

#### 1.2 Test Multi-Role Login

```http
# Test 5: Login as STUDENT
POST http://localhost:8080/auth/login
{
  "email": "john.student@example.com",
  "password": "password123"
}

Expected Response:
{
  "userId": 4,
  "name": "John Student",
  "email": "john.student@example.com",
  "role": "STUDENT",
  "studentId": 3
}
```

```http
# Test 6: Login as ADMIN
POST http://localhost:8080/auth/login
{
  "email": "admin@ishine.com",
  "password": "admin123"
}

Expected Response:
{
  "userId": 3,
  "name": "Admin",
  "email": "admin@ishine.com",
  "role": "ADMIN",
  "studentId": null
}
```

---

### Phase 2: User Management Testing

**File:** `users/users.http`

#### 2.1 Query Users

```http
# Test 7: Get all active users
GET http://localhost:8080/api/users/active

Expected: Array of active users
```

```http
# Test 8: Get users by role
GET http://localhost:8080/api/users/role/STUDENT

Expected: Array of student users
```

```http
# Test 9: Get user by ID
GET http://localhost:8080/api/users/4

Expected: Single user object
```

#### 2.2 Update User Profile

```http
# Test 10: Update profile
PUT http://localhost:8080/api/users/4/profile
{
  "name": "John Updated",
  "email": "john.updated@example.com"
}

Expected: Updated user object
```

#### 2.3 Change Password

```http
# Test 11: Change password
PUT http://localhost:8080/api/users/4/password
{
  "currentPassword": "password123",
  "newPassword": "newpassword456"
}

Expected: 204 No Content
```

---

### Phase 3: Parent-Student Relationship Testing

**File:** `parents/parents.http`

**Setup:** Use userId from parent signup (e.g., 5) and student userId (e.g., 4)

#### 3.1 Link Parent to Student

```http
# Test 12: Link parent to student
POST http://localhost:8080/api/parents/5/students
{
  "studentUserId": 4
}

Expected: 201 Created
```

#### 3.2 Query Relationships

```http
# Test 13: Get parent's students
GET http://localhost:8080/api/parents/5/students

Expected: Array of student users
[
  {
    "userId": 4,
    "name": "John Student",
    "email": "john.student@example.com",
    "role": "STUDENT",
    "isActive": true
  }
]
```

```http
# Test 14: Get student's parents
GET http://localhost:8080/api/parents/students/4/parents

Expected: Array of parent users
```

```http
# Test 15: Check if linked
GET http://localhost:8080/api/parents/5/students/4/linked

Expected: true
```

#### 3.3 Unlink Parent from Student

```http
# Test 16: Unlink
DELETE http://localhost:8080/api/parents/5/students/4

Expected: 204 No Content
```

---

### Phase 4: Teacher-Student Relationship Testing

**File:** `teachers/teachers.http`

**Setup:** Use userId from teacher signup (e.g., 6) and student userId (e.g., 4)

Follow same pattern as parent tests:
- Test 17: Link teacher to student
- Test 18: Get teacher's students
- Test 19: Get student's teachers
- Test 20: Check if linked
- Test 21: Unlink

---

### Phase 5: Custom Question Testing

**File:** `customquestions/customquestions.http`

**Setup:** Use parent or teacher userId (e.g., 5)

#### 5.1 Create Custom Question

```http
# Test 22: Create custom question
POST http://localhost:8080/questions/custom?creatorUserId=5
{
  "chapterId": "LC5H0102",
  "questionText": "What is 2 + 2?",
  "optionA": "3",
  "optionB": "4",
  "optionC": "5",
  "optionD": "6",
  "correctAnswer": "B",
  "questionType": 1,
  "difficultyLevel": "Easy",
  "explanation": "Basic addition",
  "usageType": "Practice"
}

Expected: Question object with:
- createdBy: User object
- isCustom: true
- visibility: "PRIVATE"
```

#### 5.2 Query Custom Questions

```http
# Test 23: Get creator's questions
GET http://localhost:8080/questions/custom/creator/5

Expected: Array of questions created by user 5
```

```http
# Test 24: Get creator's questions by chapter
GET http://localhost:8080/questions/custom/creator/5/chapter/LC5H0102

Expected: Filtered array of questions
```

#### 5.3 Update Question Visibility

```http
# Test 25: Make question public
PUT http://localhost:8080/questions/331/visibility?creatorUserId=5&visibility=PUBLIC

Expected: Updated question with visibility="PUBLIC"
```

#### 5.4 Delete Custom Question

```http
# Test 26: Delete custom question
DELETE http://localhost:8080/questions/custom/331?creatorUserId=5

Expected: 200 OK
```

#### 5.5 Query System vs Custom Questions

```http
# Test 27: Get all system questions
GET http://localhost:8080/questions/system

Expected: Array of questions with isCustom=false
```

```http
# Test 28: Get all custom questions (admin only)
GET http://localhost:8080/questions/all-custom

Expected: Array of questions with isCustom=true
```

---

## ✅ Verification Checklist

After running all tests, verify:

- [ ] Students can signup and login
- [ ] Parents can signup and login
- [ ] Teachers can signup and login
- [ ] Admin cannot signup through public endpoint
- [ ] Admin can login with existing credentials
- [ ] Users can update their profiles
- [ ] Users can change passwords
- [ ] Parents can link to students
- [ ] Teachers can link to students
- [ ] Relationships can be queried
- [ ] Relationships can be unlinked
- [ ] Parents/teachers can create custom questions
- [ ] Custom questions are marked correctly (isCustom=true)
- [ ] Question visibility can be updated
- [ ] Only creators can delete their custom questions
- [ ] System questions remain separate

---

## 🐛 Common Issues & Solutions

### Issue 1: 404 Not Found
**Solution:** Ensure application is running on port 8080

### Issue 2: 401 Unauthorized
**Solution:** Check if user exists and password is correct

### Issue 3: 403 Forbidden
**Solution:** Verify user has correct role for the operation

### Issue 4: 409 Conflict
**Solution:** Email already exists or relationship already created

### Issue 5: Column not found
**Solution:** Run database migration scripts first

---

## 📊 Expected Database State After Testing

**Users Table:**
- 3 existing users (2 students + 1 admin from migration)
- 3 new users (1 student + 1 parent + 1 teacher from tests)
- Total: 6 users

**Questions Table:**
- 330 system questions (isCustom=false)
- 1+ custom questions (isCustom=true)

**Parent_Student Table:**
- 1+ relationship records

**Teacher_Student Table:**
- 1+ relationship records

---

## 🎯 Next Steps

After successful testing:
1. Document any issues found
2. Test error scenarios
3. Implement frontend integration
4. Add authentication/authorization middleware
5. Deploy to production

---

**Made with Bob** 🤖