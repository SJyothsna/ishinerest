# Test Assignment API Documentation

## Overview
This document describes the API endpoints for assigning tests to students and managing test assignments. Both teachers and parents can create tests and assign them to their students.

---

## Table of Contents
1. [Assign Test to Students](#1-assign-test-to-students)
2. [Get Test Assignments](#2-get-test-assignments)
3. [Delete Test Assignment](#3-delete-test-assignment)
4. [Get Student's Assigned Tests](#4-get-students-assigned-tests)
5. [Get Specific Assignment](#5-get-specific-assignment)
6. [Start Test](#6-start-test)
7. [Complete Test](#7-complete-test)
8. [Database Migration](#8-database-migration)

---

## 1. Assign Test to Students

Assign a test to one or more students. Only teachers and parents can assign tests to students they have relationships with.

### Endpoint
```
POST /users/{userId}/tests/assign
```

### Path Parameters
- `userId` (Long, required): The user ID of the teacher or parent assigning the test

### Request Body
```json
{
  "testId": 1,
  "studentUserIds": [14, 15, 16],
  "dueDate": "2026-05-15T23:59:59"
}
```

### Request Fields
- `testId` (Long, required): ID of the test to assign
- `studentUserIds` (List<Long>, required): List of student user IDs to assign the test to
- `dueDate` (LocalDateTime, optional): Due date for the test

### Response (201 Created)
```json
[
  {
    "assignmentId": 1,
    "testId": 1,
    "testTitle": "Math Quiz - Chapter 5",
    "studentUserId": 14,
    "studentName": "Lasya Maddala",
    "studentEmail": "lasya.maddala@gmail.com",
    "assignedByUserId": 9,
    "assignedByName": "Jyothsna Sirasanameti",
    "assignedAt": "2026-05-09T20:00:00",
    "dueDate": "2026-05-15T23:59:59",
    "startedAt": null,
    "completedAt": null,
    "score": null,
    "status": "ASSIGNED",
    "feedback": null
  },
  {
    "assignmentId": 2,
    "testId": 1,
    "testTitle": "Math Quiz - Chapter 5",
    "studentUserId": 15,
    "studentName": "John Doe",
    "studentEmail": "john.doe@gmail.com",
    "assignedByUserId": 9,
    "assignedByName": "Jyothsna Sirasanameti",
    "assignedAt": "2026-05-09T20:00:00",
    "dueDate": "2026-05-15T23:59:59",
    "startedAt": null,
    "completedAt": null,
    "score": null,
    "status": "ASSIGNED",
    "feedback": null
  }
]
```

### Status Values
- `ASSIGNED`: Test has been assigned but not started
- `IN_PROGRESS`: Student has started the test
- `COMPLETED`: Student has completed the test
- `OVERDUE`: Test is past due date (future enhancement)

### Validation Rules
- Test must exist and belong to the user
- Test must be published (`isPublished = true`)
- All student user IDs must exist and have STUDENT role
- Teacher/Parent must have an active relationship with each student
- Test cannot already be assigned to a student (unique constraint)

### Error Responses

**404 Not Found** - User not found
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "User not found"
}
```

**404 Not Found** - Test not found or no permission
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Test not found or you don't have permission to assign it"
}
```

**400 Bad Request** - Test not published
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot assign unpublished test. Please publish the test first."
}
```

**400 Bad Request** - Student not found
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Student with userId 14 not found"
}
```

**400 Bad Request** - User is not a student
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "User 14 is not a student"
}
```

**403 Forbidden** - No relationship with student
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "You don't have a relationship with student 14"
}
```

**400 Bad Request** - Test already assigned
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Test is already assigned to student 14"
}
```

### cURL Example
```bash
curl -X POST http://localhost:8080/users/9/tests/assign \
  -H "Content-Type: application/json" \
  -d '{
    "testId": 1,
    "studentUserIds": [14, 15, 16],
    "dueDate": "2026-05-15T23:59:59"
  }'
```

---

## 2. Get Test Assignments

Get all assignments for a specific test (who the test has been assigned to).

### Endpoint
```
GET /users/{userId}/tests/{testId}/assignments
```

### Path Parameters
- `userId` (Long, required): The user ID of the teacher or parent who created the test
- `testId` (Long, required): The test ID

### Response (200 OK)
```json
[
  {
    "assignmentId": 1,
    "testId": 1,
    "testTitle": "Math Quiz - Chapter 5",
    "studentUserId": 14,
    "studentName": "Lasya Maddala",
    "studentEmail": "lasya.maddala@gmail.com",
    "assignedByUserId": 9,
    "assignedByName": "Jyothsna Sirasanameti",
    "assignedAt": "2026-05-09T20:00:00",
    "dueDate": "2026-05-15T23:59:59",
    "startedAt": "2026-05-10T10:30:00",
    "completedAt": null,
    "score": null,
    "status": "IN_PROGRESS",
    "feedback": null
  },
  {
    "assignmentId": 2,
    "testId": 1,
    "testTitle": "Math Quiz - Chapter 5",
    "studentUserId": 15,
    "studentName": "John Doe",
    "studentEmail": "john.doe@gmail.com",
    "assignedByUserId": 9,
    "assignedByName": "Jyothsna Sirasanameti",
    "assignedAt": "2026-05-09T20:00:00",
    "dueDate": "2026-05-15T23:59:59",
    "startedAt": "2026-05-11T14:20:00",
    "completedAt": "2026-05-11T15:05:00",
    "score": 85,
    "status": "COMPLETED",
    "feedback": null
  }
]
```

### Error Responses

**404 Not Found** - User not found
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "User not found"
}
```

**404 Not Found** - Test not found or no permission
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Test not found or you don't have permission to view its assignments"
}
```

### cURL Example
```bash
curl -X GET http://localhost:8080/users/9/tests/1/assignments
```

---

## 3. Delete Test Assignment

Delete a test assignment (unassign a test from a student).

### Endpoint
```
DELETE /users/{userId}/tests/assignments/{assignmentId}
```

### Path Parameters
- `userId` (Long, required): The user ID of the teacher or parent who created the assignment
- `assignmentId` (Long, required): The assignment ID to delete

### Response (204 No Content)
No response body

### Error Responses

**404 Not Found** - Assignment not found
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Assignment not found"
}
```

**403 Forbidden** - No permission to delete
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "You don't have permission to delete this assignment"
}
```

### cURL Example
```bash
curl -X DELETE http://localhost:8080/users/9/tests/assignments/1
```

---

## 4. Get Student's Assigned Tests

Get all tests assigned to a specific student.

### Endpoint
```
GET /students/{studentUserId}/assigned-tests
```

### Path Parameters
- `studentUserId` (Long, required): The student's user ID

### Response (200 OK)
```json
[
  {
    "assignmentId": 1,
    "testId": 1,
    "testTitle": "Math Quiz - Chapter 5",
    "studentUserId": 14,
    "studentName": "Lasya Maddala",
    "studentEmail": "lasya.maddala@gmail.com",
    "assignedByUserId": 9,
    "assignedByName": "Jyothsna Sirasanameti",
    "assignedAt": "2026-05-09T20:00:00",
    "dueDate": "2026-05-15T23:59:59",
    "startedAt": null,
    "completedAt": null,
    "score": null,
    "status": "ASSIGNED",
    "feedback": null
  },
  {
    "assignmentId": 3,
    "testId": 2,
    "testTitle": "Science Test - Biology",
    "studentUserId": 14,
    "studentName": "Lasya Maddala",
    "studentEmail": "lasya.maddala@gmail.com",
    "assignedByUserId": 10,
    "assignedByName": "Teacher Name",
    "assignedAt": "2026-05-08T15:00:00",
    "dueDate": "2026-05-12T23:59:59",
    "startedAt": "2026-05-09T10:00:00",
    "completedAt": "2026-05-09T11:30:00",
    "score": 92,
    "status": "COMPLETED",
    "feedback": null
  }
]
```

### Error Responses

**404 Not Found** - Student not found
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Student not found"
}
```

**400 Bad Request** - User is not a student
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "User is not a student"
}
```

### cURL Example
```bash
curl -X GET http://localhost:8080/students/14/assigned-tests
```

---

## 5. Get Specific Assignment

Get details of a specific test assignment.

### Endpoint
```
GET /students/{studentUserId}/assigned-tests/{assignmentId}
```

### Path Parameters
- `studentUserId` (Long, required): The student's user ID
- `assignmentId` (Long, required): The assignment ID

### Response (200 OK)
```json
{
  "assignmentId": 1,
  "testId": 1,
  "testTitle": "Math Quiz - Chapter 5",
  "studentUserId": 14,
  "studentName": "Lasya Maddala",
  "studentEmail": "lasya.maddala@gmail.com",
  "assignedByUserId": 9,
  "assignedByName": "Jyothsna Sirasanameti",
  "assignedAt": "2026-05-09T20:00:00",
  "dueDate": "2026-05-15T23:59:59",
  "startedAt": null,
  "completedAt": null,
  "score": null,
  "status": "ASSIGNED",
  "feedback": null
}
```

### Error Responses

**404 Not Found** - Assignment not found
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Assignment not found"
}
```

### cURL Example
```bash
curl -X GET http://localhost:8080/students/14/assigned-tests/1
```

---

## 6. Start Test

Mark a test as started (changes status from ASSIGNED to IN_PROGRESS).

### Endpoint
```
POST /students/{studentUserId}/assigned-tests/{assignmentId}/start
```

### Path Parameters
- `studentUserId` (Long, required): The student's user ID
- `assignmentId` (Long, required): The assignment ID

### Request Body
No request body required

### Response (200 OK)
```json
{
  "assignmentId": 1,
  "testId": 1,
  "testTitle": "Math Quiz - Chapter 5",
  "studentUserId": 14,
  "studentName": "Lasya Maddala",
  "studentEmail": "lasya.maddala@gmail.com",
  "assignedByUserId": 9,
  "assignedByName": "Jyothsna Sirasanameti",
  "assignedAt": "2026-05-09T20:00:00",
  "dueDate": "2026-05-15T23:59:59",
  "startedAt": "2026-05-10T10:30:00",
  "completedAt": null,
  "score": null,
  "status": "IN_PROGRESS",
  "feedback": null
}
```

### Error Responses

**404 Not Found** - Assignment not found
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Assignment not found"
}
```

**403 Forbidden** - Not student's assignment
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "You don't have permission to start this test"
}
```

**400 Bad Request** - Test already started
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Test has already been started or completed"
}
```

### cURL Example
```bash
curl -X POST http://localhost:8080/students/14/assigned-tests/1/start
```

---

## 7. Complete Test

Submit a completed test with a score.

### Endpoint
```
POST /students/{studentUserId}/assigned-tests/{assignmentId}/complete
```

### Path Parameters
- `studentUserId` (Long, required): The student's user ID
- `assignmentId` (Long, required): The assignment ID

### Request Body
```json
{
  "score": 85
}
```

### Request Fields
- `score` (Integer, required): Test score (0-100)

### Response (200 OK)
```json
{
  "assignmentId": 1,
  "testId": 1,
  "testTitle": "Math Quiz - Chapter 5",
  "studentUserId": 14,
  "studentName": "Lasya Maddala",
  "studentEmail": "lasya.maddala@gmail.com",
  "assignedByUserId": 9,
  "assignedByName": "Jyothsna Sirasanameti",
  "assignedAt": "2026-05-09T20:00:00",
  "dueDate": "2026-05-15T23:59:59",
  "startedAt": "2026-05-10T10:30:00",
  "completedAt": "2026-05-10T11:45:00",
  "score": 85,
  "status": "COMPLETED",
  "feedback": null
}
```

### Error Responses

**404 Not Found** - Assignment not found
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Assignment not found"
}
```

**403 Forbidden** - Not student's assignment
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "You don't have permission to complete this test"
}
```

**400 Bad Request** - Test already completed
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Test is already completed"
}
```

**400 Bad Request** - Invalid score
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Score must be between 0 and 100"
}
```

### cURL Example
```bash
curl -X POST http://localhost:8080/students/14/assigned-tests/1/complete \
  -H "Content-Type: application/json" \
  -d '{"score": 85}'
```

---

## 8. Database Migration

Before using the test assignment feature, you must run the database migration to create the `test_assignments` table.

### Migration File
`database_migration_create_test_assignments_table.sql`

### Run Migration
```sql
-- Execute the SQL file in your MySQL database
source database_migration_create_test_assignments_table.sql;
```

### Table Structure
```sql
CREATE TABLE test_assignments (
    assignment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_id BIGINT NOT NULL,
    student_user_id BIGINT NOT NULL,
    assigned_by_user_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_date TIMESTAMP NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    score INT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',
    feedback TEXT NULL,
    
    FOREIGN KEY (test_id) REFERENCES teacher_tests(test_id) ON DELETE CASCADE,
    FOREIGN KEY (student_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_by_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    UNIQUE KEY uk_test_student (test_id, student_user_id)
);
```

---

## Complete Workflow Example

### 1. Teacher/Parent Creates a Test
```bash
POST /users/9/tests
{
  "title": "Math Quiz - Chapter 5",
  "description": "Test on algebra basics",
  "subjectId": "LC5M",
  "chapterId": "LC5M0105",
  "durationMinutes": 60,
  "isPublished": true,
  "questionIds": [1, 2, 3, 4, 5]
}
```

### 2. Assign Test to Students
```bash
POST /users/9/tests/assign
{
  "testId": 1,
  "studentUserIds": [14, 15],
  "dueDate": "2026-05-15T23:59:59"
}
```

### 3. Student Views Assigned Tests
```bash
GET /students/14/assigned-tests
```

### 4. Student Starts Test
```bash
POST /students/14/assigned-tests/1/start
```

### 5. Student Completes Test
```bash
POST /students/14/assigned-tests/1/complete
{
  "score": 85
}
```

### 6. Teacher/Parent Views Results
```bash
GET /users/9/tests/1/assignments
```

---

## Notes

### Permissions
- **Teachers**: Can assign tests to students they have a relationship with (via `teacher_student` table)
- **Parents**: Can assign tests to students they have an ACTIVE relationship with (via `parent_student` table with status='ACTIVE')
- **Students**: Can only view, start, and complete tests assigned to them

### Business Rules
1. Tests must be published before they can be assigned
2. A test can only be assigned once to each student (enforced by unique constraint)
3. Students must start a test before completing it (status progression: ASSIGNED → IN_PROGRESS → COMPLETED)
4. Scores must be between 0 and 100
5. Deleting a test will cascade delete all its assignments
6. Deleting a user will cascade delete all their assignments (as student or assigner)

### Future Enhancements
- Auto-update status to OVERDUE based on due date
- Add feedback field for teacher/parent comments
- Add attempt tracking (allow multiple attempts)
- Add time tracking (actual time spent on test)
- Add detailed answer tracking (which questions were answered correctly)

---

Made with Bob