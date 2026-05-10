# Test Creation API Documentation for UI Design

## Overview
Generic test creation API that works for **both teachers and parents**. Users can create custom tests by selecting questions.

---

## Endpoints

### 1. Create Test
**POST** `/users/{userId}/tests`

Creates a new test for the logged-in user (teacher or parent).

#### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| userId | Long | Yes | The ID of the user creating the test |

#### Request Body
```json
{
  "title": "Math Quiz - Chapter 5",
  "description": "Practice test for algebra basics",
  "subjectId": "LC5H01",
  "chapterId": "LC5H0105",
  "durationMinutes": 60,
  "isPublished": false,
  "questionIds": [123, 456, 789, 101, 112]
}
```

#### Request Body Fields
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| title | String | Yes | Test title (e.g., "Math Quiz - Chapter 5") |
| description | String | No | Optional description of the test |
| subjectId | String | No | Subject ID (e.g., "LC5H01" for Maths) |
| chapterId | String | No | Chapter ID (e.g., "LC5H0105") |
| durationMinutes | Integer | No | Time limit in minutes (e.g., 60) |
| isPublished | Boolean | Yes | Whether test is published (true/false) |
| questionIds | Array[Long] | Yes | List of question IDs to include (min 1) |

#### Response (201 Created)
```json
{
  "testId": 42,
  "title": "Math Quiz - Chapter 5",
  "description": "Practice test for algebra basics",
  "subjectId": "LC5H01",
  "chapterId": "LC5H0105",
  "durationMinutes": 60,
  "isPublished": false,
  "createdAt": "2026-05-09T12:30:00",
  "createdBy": {
    "userId": 9,
    "name": "John Parent",
    "email": "john.parent@example.com",
    "role": "PARENT"
  },
  "questions": [
    {
      "questionId": 123,
      "questionText": "What is 2 + 2?",
      "questionType": 1,
      "questionTypeName": "SINGLE_CHOICE",
      "chapterId": "LC5H0105",
      "subjectId": "LC5H01",
      "displayOrder": 1
    },
    {
      "questionId": 456,
      "questionText": "Solve: x + 5 = 10",
      "questionType": 1,
      "questionTypeName": "SINGLE_CHOICE",
      "chapterId": "LC5H0105",
      "subjectId": "LC5H01",
      "displayOrder": 2
    },
    {
      "questionId": 789,
      "questionText": "Is 3 > 5?",
      "questionType": 3,
      "questionTypeName": "TRUE_FALSE",
      "chapterId": "LC5H0105",
      "subjectId": "LC5H01",
      "displayOrder": 3
    }
  ]
}
```

#### Response Fields
| Field | Type | Description |
|-------|------|-------------|
| testId | Long | Unique test identifier |
| title | String | Test title |
| description | String | Test description |
| subjectId | String | Subject ID |
| chapterId | String | Chapter ID |
| durationMinutes | Integer | Time limit in minutes |
| isPublished | Boolean | Publication status |
| createdAt | DateTime | Creation timestamp (ISO 8601) |
| createdBy | Object | User who created the test |
| createdBy.userId | Long | Creator's user ID |
| createdBy.name | String | Creator's name |
| createdBy.email | String | Creator's email |
| createdBy.role | String | Creator's role (TEACHER/PARENT) |
| questions | Array | List of questions in the test |
| questions[].questionId | Long | Question ID |
| questions[].questionText | String | Question text |
| questions[].questionType | Integer | Question type code (1-5) |
| questions[].questionTypeName | String | Question type name |
| questions[].chapterId | String | Chapter ID |
| questions[].subjectId | String | Subject ID |
| questions[].displayOrder | Integer | Display order in test |

#### Question Types
| Code | Name | Description |
|------|------|-------------|
| 1 | SINGLE_CHOICE | Multiple choice (one answer) |
| 2 | MULTIPLE_CHOICE | Multiple choice (multiple answers) |
| 3 | TRUE_FALSE | True/False question |
| 4 | FILL_IN_THE_BLANK | Fill in the blank |
| 5 | SHORT_ANSWER | Short answer question |

#### Error Responses

**400 Bad Request** - Invalid input
```json
{
  "timestamp": "2026-05-09T12:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "One or more questionIds are invalid",
  "path": "/users/9/tests"
}
```

**404 Not Found** - User not found
```json
{
  "timestamp": "2026-05-09T12:30:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "message": "User not found",
  "path": "/users/999/tests"
}
```

---

### 2. Get All Tests for User
**GET** `/users/{userId}/tests`

Retrieves all tests created by a specific user.

#### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| userId | Long | Yes | The ID of the user |

#### Response (200 OK)
```json
[
  {
    "testId": 42,
    "title": "Math Quiz - Chapter 5",
    "description": "Practice test for algebra basics",
    "subjectId": "LC5H01",
    "chapterId": "LC5H0105",
    "durationMinutes": 60,
    "isPublished": false,
    "createdAt": "2026-05-09T12:30:00",
    "createdBy": {
      "userId": 9,
      "name": "John Parent",
      "email": "john.parent@example.com",
      "role": "PARENT"
    },
    "questions": [
      {
        "questionId": 123,
        "questionText": "What is 2 + 2?",
        "questionType": 1,
        "questionTypeName": "SINGLE_CHOICE",
        "chapterId": "LC5H0105",
        "subjectId": "LC5H01",
        "displayOrder": 1
      }
    ]
  },
  {
    "testId": 43,
    "title": "Science Quiz",
    "description": "Biology basics",
    "subjectId": "LC5H02",
    "chapterId": null,
    "durationMinutes": 45,
    "isPublished": true,
    "createdAt": "2026-05-08T10:15:00",
    "createdBy": {
      "userId": 9,
      "name": "John Parent",
      "email": "john.parent@example.com",
      "role": "PARENT"
    },
    "questions": [...]
  }
]
```

---

### 3. Get Test by ID
**GET** `/users/{userId}/tests/{testId}`

Retrieves a specific test by ID for a user.

#### Path Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| userId | Long | Yes | The ID of the user |
| testId | Long | Yes | The ID of the test |

#### Response (200 OK)
```json
{
  "testId": 42,
  "title": "Math Quiz - Chapter 5",
  "description": "Practice test for algebra basics",
  "subjectId": "LC5H01",
  "chapterId": "LC5H0105",
  "durationMinutes": 60,
  "isPublished": false,
  "createdAt": "2026-05-09T12:30:00",
  "createdBy": {
    "userId": 9,
    "name": "John Parent",
    "email": "john.parent@example.com",
    "role": "PARENT"
  },
  "questions": [
    {
      "questionId": 123,
      "questionText": "What is 2 + 2?",
      "questionType": 1,
      "questionTypeName": "SINGLE_CHOICE",
      "chapterId": "LC5H0105",
      "subjectId": "LC5H01",
      "displayOrder": 1
    }
  ]
}
```

#### Error Response (404 Not Found)
```json
{
  "timestamp": "2026-05-09T12:30:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Test not found",
  "path": "/users/9/tests/999"
}
```

---

## Usage Examples

### Example 1: Parent Creates a Test
```bash
POST http://localhost:8080/users/9/tests
Content-Type: application/json

{
  "title": "Weekly Math Practice",
  "description": "Practice for my child",
  "subjectId": "LC5H01",
  "chapterId": "LC5H0105",
  "durationMinutes": 30,
  "isPublished": false,
  "questionIds": [123, 456, 789]
}
```

### Example 2: Teacher Creates a Test
```bash
POST http://localhost:8080/users/15/tests
Content-Type: application/json

{
  "title": "Mid-term Exam",
  "description": "Covers chapters 1-5",
  "subjectId": "LC5H01",
  "chapterId": null,
  "durationMinutes": 90,
  "isPublished": true,
  "questionIds": [101, 102, 103, 104, 105, 106, 107, 108, 109, 110]
}
```

### Example 3: Get All Tests
```bash
GET http://localhost:8080/users/9/tests
```

### Example 4: Get Specific Test
```bash
GET http://localhost:8080/users/9/tests/42
```

---

## Validation Rules

### For All Users (Teachers & Parents)
1. **Title**: Required, cannot be blank
2. **isPublished**: Required, must be true or false
3. **questionIds**: Required, must contain at least 1 question ID
4. **No duplicate questions**: Same question cannot appear twice
5. **Valid questions**: All question IDs must exist in database
6. **Question ownership**: Can only use:
   - Admin/system questions
   - Own custom questions

### Additional Rules for Teachers Only
7. **Subject permissions**: Teacher must be assigned to the subject
8. **Chapter permissions**: Teacher must be assigned to the chapter's subject

### No Restrictions for Parents
- Parents can create tests for any subject/chapter
- Parents can use any admin questions or their own custom questions

---

## UI Design Recommendations

### Create Test Form
```
┌─────────────────────────────────────────┐
│ Create New Test                         │
├─────────────────────────────────────────┤
│                                         │
│ Title: [_____________________________] │
│                                         │
│ Description (optional):                 │
│ [___________________________________]  │
│ [___________________________________]  │
│                                         │
│ Subject: [Select Subject ▼]            │
│ Chapter: [Select Chapter ▼]            │
│                                         │
│ Duration: [__] minutes                  │
│                                         │
│ Status: ○ Draft  ● Published           │
│                                         │
│ Questions:                              │
│ ┌─────────────────────────────────┐   │
│ │ [Search questions...]           │   │
│ │                                 │   │
│ │ ☑ Q123: What is 2 + 2?         │   │
│ │ ☑ Q456: Solve: x + 5 = 10      │   │
│ │ ☐ Q789: Is 3 > 5?              │   │
│ │ ☐ Q101: Calculate 10 × 5       │   │
│ └─────────────────────────────────┘   │
│                                         │
│ Selected: 2 questions                   │
│                                         │
│ [Cancel]              [Create Test]    │
└─────────────────────────────────────────┘
```

### Test List View
```
┌─────────────────────────────────────────┐
│ My Tests                    [+ New Test]│
├─────────────────────────────────────────┤
│                                         │
│ ┌─────────────────────────────────────┐│
│ │ Math Quiz - Chapter 5        DRAFT  ││
│ │ 5 questions • 60 min                ││
│ │ Created: May 9, 2026                ││
│ │ [Edit] [Delete] [Publish]           ││
│ └─────────────────────────────────────┘│
│                                         │
│ ┌─────────────────────────────────────┐│
│ │ Science Quiz          PUBLISHED     ││
│ │ 10 questions • 45 min               ││
│ │ Created: May 8, 2026                ││
│ │ [View] [Edit] [Unpublish]           ││
│ └─────────────────────────────────────┘│
│                                         │
└─────────────────────────────────────────┘
```

---

## Notes for Frontend Developers

1. **User ID**: Get from logged-in user session/token
2. **Question Selection**: Fetch available questions from questions API
3. **Subject/Chapter**: Optional fields, can be left null
4. **Duration**: Optional, can be left null for untimed tests
5. **isPublished**: 
   - `false` = Draft (only creator can see)
   - `true` = Published (can be assigned to students)
6. **Question Order**: Questions appear in the order of questionIds array
7. **Error Handling**: Display validation errors from API responses

---

**Last Updated:** 2026-05-09
**API Version:** 1.0
**Base URL:** `http://localhost:8080`