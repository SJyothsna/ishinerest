# Test Management API Specification

## Overview
Comprehensive test management system covering test creation, assignment, and completion. Includes three main controllers: TeacherTestController, TestAssignmentController, and TestDetailController.

---

## TeacherTestController

### Base URL
`/users/{userId}/tests`

### Overview
Allows teachers and parents to create custom tests and assign them to students.

---

### Endpoints

#### 1. Create Test
**POST** `/users/{userId}/tests`

Creates a new test.

**Path Parameters:**
- `userId` (required): Creator's user ID (teacher or parent)

**Request Body:**
```json
{
  "title": "Chapter 1 Quiz",
  "description": "Basic arithmetic quiz",
  "subjectId": "LC5H01",
  "chapterId": "LC5H0101",
  "questionIds": [1, 2, 3, 4, 5],
  "timeLimit": 30,
  "passingScore": 70
}
```

**Response (201 Created):**
```json
{
  "testId": 1,
  "title": "Chapter 1 Quiz",
  "description": "Basic arithmetic quiz",
  "creatorUserId": 5,
  "creatorName": "Mr. Anderson",
  "subjectId": "LC5H01",
  "subjectName": "Mathematics",
  "chapterId": "LC5H0101",
  "chapterName": "Number Systems",
  "questionCount": 5,
  "timeLimit": 30,
  "passingScore": 70,
  "createdAt": "2024-01-20T10:00:00Z"
}
```

---

#### 2. Get User Tests
**GET** `/users/{userId}/tests`

Retrieves all tests created by a user.

**Response (200 OK):** Array of TeacherTestResponseDTO

---

#### 3. Get Test by ID
**GET** `/users/{userId}/tests/{testId}`

**Response (200 OK):** Single TeacherTestResponseDTO

---

#### 4. Assign Test
**POST** `/users/{userId}/tests/assign`

Assigns a test to one or more students.

**Request Body:**
```json
{
  "testId": 1,
  "studentUserIds": [15, 16, 17],
  "dueDate": "2024-01-25T23:59:59Z",
  "instructions": "Complete before Friday"
}
```

**Response (201 Created):**
```json
[
  {
    "assignmentId": 1,
    "testId": 1,
    "testTitle": "Chapter 1 Quiz",
    "studentUserId": 15,
    "studentName": "John Doe",
    "assignedBy": 5,
    "assignedByName": "Mr. Anderson",
    "assignedAt": "2024-01-20T10:00:00Z",
    "dueDate": "2024-01-25T23:59:59Z",
    "status": "ASSIGNED",
    "score": null,
    "completedAt": null
  }
]
```

---

#### 5. Get Test Assignments
**GET** `/users/{userId}/tests/{testId}/assignments`

Gets all assignments for a specific test.

**Response (200 OK):** Array of TestAssignmentDTO

---

#### 6. Delete Assignment
**DELETE** `/users/{userId}/tests/assignments/{assignmentId}`

Removes a test assignment.

**Response (204 No Content)**

---

## TestAssignmentController

### Base URL
`/students/{studentUserId}/assigned-tests`

### Overview
Student-facing endpoints for viewing and completing assigned tests.

---

### Endpoints

#### 1. Get Student Assignments
**GET** `/students/{studentUserId}/assigned-tests`

Retrieves all tests assigned to a student.

**Response (200 OK):**
```json
[
  {
    "assignmentId": 1,
    "testId": 1,
    "testTitle": "Chapter 1 Quiz",
    "studentUserId": 15,
    "studentName": "John Doe",
    "assignedBy": 5,
    "assignedByName": "Mr. Anderson",
    "assignedAt": "2024-01-20T10:00:00Z",
    "dueDate": "2024-01-25T23:59:59Z",
    "status": "ASSIGNED",
    "score": null,
    "completedAt": null,
    "timeLimit": 30,
    "questionCount": 5
  }
]
```

---

#### 2. Get Assignment by ID
**GET** `/students/{studentUserId}/assigned-tests/{assignmentId}`

**Response (200 OK):** Single TestAssignmentDTO

---

#### 3. Start Test
**POST** `/students/{studentUserId}/assigned-tests/{assignmentId}/start`

Marks a test as in progress.

**Response (200 OK):**
```json
{
  "assignmentId": 1,
  "status": "IN_PROGRESS",
  "startedAt": "2024-01-22T14:30:00Z"
}
```

---

#### 4. Complete Test
**POST** `/students/{studentUserId}/assigned-tests/{assignmentId}/complete`

Submits a completed test with score.

**Request Body:**
```json
{
  "score": 85
}
```

**Response (200 OK):**
```json
{
  "assignmentId": 1,
  "status": "COMPLETED",
  "score": 85,
  "completedAt": "2024-01-22T15:00:00Z",
  "passed": true
}
```

---

## TestDetailController

### Base URL
`/test-details`

### Overview
Manages individual question attempts within tests, tracking answers and results.

---

### Endpoints

#### 1. Get All Test Details
**GET** `/test-details`

**Response (200 OK):** Array of all test details

---

#### 2. Get Test Details by Student
**GET** `/test-details/{studentId}`

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "studentId": 1,
    "assignmentId": 1,
    "questionId": 1,
    "selectedAnswer": "B",
    "isCorrect": true,
    "attemptedAt": "2024-01-22T14:35:00Z"
  }
]
```

---

#### 3. Get Test Details by Student and Assignment
**GET** `/test-details/student/{studentId}/assignment/{assignmentId}`

**Response (200 OK):** Array of test details for specific assignment

---

#### 4. Create Test Details
**POST** `/test-details/{studentId}`

Records student's answers for a test.

**Request Body:**
```json
[
  {
    "assignmentId": 1,
    "questionId": 1,
    "selectedAnswer": "B",
    "isCorrect": true
  },
  {
    "assignmentId": 1,
    "questionId": 2,
    "selectedAnswer": "A",
    "isCorrect": false
  }
]
```

**Response (200 OK):** Array of created test details

---

#### 5. Delete Test Detail
**DELETE** `/test-details/{id}`

**Response (200 OK)**

---

#### 6. Reset Student Progress
**DELETE** `/test-details/reset/student/{studentId}`

Resets all test progress for a student.

**Response (200 OK):**
```json
"Progress reset successfully for student 1"
```

---

#### 7. Reset Assignment Progress
**DELETE** `/test-details/reset/student/{studentId}/assignment/{assignmentId}`

Resets progress for a specific assignment.

**Response (200 OK):**
```json
"Progress reset successfully for student 1 in assignment 1"
```

---

#### 8. Get Wrong Answers by Assignment
**GET** `/test-details/wrong-answers/student/{studentId}/assignment/{assignmentId}`

**Response (200 OK):**
```json
[
  {
    "id": 2,
    "questionText": "What is 5 + 5?",
    "selectedAnswer": "A",
    "correctAnswer": "B",
    "isFlagged": false
  }
]
```

---

## Data Models

### CreateTeacherTestRequest
```json
{
  "title": "string (required)",
  "description": "string (optional)",
  "subjectId": "string (required)",
  "chapterId": "string (optional)",
  "questionIds": "array of long (required)",
  "timeLimit": "integer (minutes, optional)",
  "passingScore": "integer (0-100, optional)"
}
```

### TeacherTestResponseDTO
```json
{
  "testId": "long",
  "title": "string",
  "description": "string",
  "creatorUserId": "long",
  "creatorName": "string",
  "subjectId": "string",
  "subjectName": "string",
  "chapterId": "string",
  "chapterName": "string",
  "questionCount": "integer",
  "timeLimit": "integer",
  "passingScore": "integer",
  "createdAt": "datetime"
}
```

### AssignTestRequest
```json
{
  "testId": "long (required)",
  "studentUserIds": "array of long (required)",
  "dueDate": "datetime (optional)",
  "instructions": "string (optional)"
}
```

### TestAssignmentDTO
```json
{
  "assignmentId": "long",
  "testId": "long",
  "testTitle": "string",
  "studentUserId": "long",
  "studentName": "string",
  "assignedBy": "long",
  "assignedByName": "string",
  "assignedAt": "datetime",
  "dueDate": "datetime",
  "status": "enum (ASSIGNED, IN_PROGRESS, COMPLETED)",
  "score": "integer (nullable)",
  "completedAt": "datetime (nullable)",
  "startedAt": "datetime (nullable)",
  "timeLimit": "integer",
  "questionCount": "integer",
  "passed": "boolean (nullable)"
}
```

### TestDetail
```json
{
  "id": "long",
  "studentId": "long",
  "assignmentId": "long",
  "questionId": "long",
  "selectedAnswer": "string",
  "isCorrect": "boolean",
  "attemptedAt": "datetime"
}
```

---

## Notes for UI Team

### Test Creation Flow
1. Teacher/parent selects questions from question bank
2. Sets test parameters (title, time limit, passing score)
3. Creates test via POST `/users/{userId}/tests`
4. Assigns to students via POST `/users/{userId}/tests/assign`

### Student Test Taking Flow
1. Student views assigned tests: GET `/students/{studentUserId}/assigned-tests`
2. Student starts test: POST `/students/{studentUserId}/assigned-tests/{assignmentId}/start`
3. Student answers questions (track locally)
4. Submit answers: POST `/test-details/{studentId}`
5. Complete test: POST `/students/{studentUserId}/assigned-tests/{assignmentId}/complete`

### Test Status
- **ASSIGNED**: Test assigned but not started
- **IN_PROGRESS**: Student has started the test
- **COMPLETED**: Test submitted and graded

### Timer Implementation
- Use `timeLimit` from assignment
- Start timer when test is started
- Auto-submit when time expires
- Show countdown in UI

### Score Calculation
- Calculate score client-side based on correct answers
- Submit final score with completion request
- Server validates and stores score

### Review Mode
- Use `/test-details/wrong-answers/...` to show incorrect answers
- Allow students to review after completion
- Show correct answers and explanations

### Teacher Dashboard
- Show all created tests
- View assignments per test
- Track completion status
- View student scores

### Student Dashboard
- Show pending assignments with due dates
- Highlight overdue tests
- Show completed tests with scores
- Display pass/fail status

### Retake Functionality
- Use reset endpoints to allow retakes
- Clear previous attempts
- Maintain assignment but reset details

### Error Handling
- Validate test has questions before creation
- Check student access before starting test
- Prevent multiple simultaneous attempts
- Handle timeout scenarios gracefully