# Student API Specification

## Base URL
`/students`

## Overview
Manages student profiles, class assignments, subject selections, and progress tracking for practice and test sessions.

---

## Endpoints

### 1. Get All Students
**GET** `/students`

Retrieves all students in the system.

#### Response (200 OK)
```json
[
  {
    "id": 1,
    "userId": 10,
    "classId": 5,
    "name": "John Doe",
    "email": "john.doe@example.com"
  },
  {
    "id": 2,
    "userId": 11,
    "classId": 6,
    "name": "Jane Smith",
    "email": "jane.smith@example.com"
  }
]
```

---

### 2. Get Student by ID
**GET** `/students/{id}`

Retrieves a specific student by their student ID.

#### Path Parameters
- `id` (required): The student ID

#### Response (200 OK)
```json
{
  "id": 1,
  "userId": 10,
  "classId": 5,
  "name": "John Doe",
  "email": "john.doe@example.com"
}
```

#### Error Responses
- **404 Not Found**: Student not found

---

### 3. Get Student Profile
**GET** `/students/{studentId}/profile`

Retrieves student profile information for onboarding checks.

#### Path Parameters
- `studentId` (required): The student ID

#### Response (200 OK)
```json
{
  "studentId": 1,
  "userId": 10,
  "name": "John Doe",
  "email": "john.doe@example.com",
  "classId": 5,
  "className": "Grade 5",
  "hasSelectedSubjects": true,
  "selectedSubjectsCount": 3,
  "isOnboardingComplete": true
}
```

---

### 4. Set Student Class
**PUT** `/students/{studentId}/class?classId={classId}`

Assigns a class to a student.

#### Path Parameters
- `studentId` (required): The student ID

#### Query Parameters
- `classId` (required): The class ID to assign

#### Response (204 No Content)
```
(Empty response body)
```

---

### 5. Replace Student Subjects
**PUT** `/students/{studentId}/subjects`

Replaces all subjects for a student with a new list.

#### Path Parameters
- `studentId` (required): The student ID

#### Request Body
```json
["LC5H01", "LC5H02", "LC5H03"]
```

#### Response (204 No Content)
```
(Empty response body)
```

---

### 6. Create Student
**POST** `/students`

Creates a new student record.

#### Request Body
```json
{
  "userId": 10,
  "classId": 5
}
```

#### Response (200 OK)
```json
{
  "id": 1,
  "userId": 10,
  "classId": 5,
  "name": "John Doe",
  "email": "john.doe@example.com"
}
```

---

### 7. Delete Student
**DELETE** `/students/{id}`

Deletes a student record.

#### Path Parameters
- `id` (required): The student ID

#### Response (200 OK)
```
(Empty response body)
```

---

### 8. Get Chapter Practice Progress
**GET** `/students/{studentId}/practiceProgress/chapter?chapterId={chapterId}`

Retrieves practice progress for a specific chapter.

#### Path Parameters
- `studentId` (required): The student ID

#### Query Parameters
- `chapterId` (required): The chapter ID

#### Response (200 OK)
```json
{
  "studentId": 1,
  "chapterId": "LC5H0101",
  "chapterName": "Number Systems",
  "totalQuestions": 50,
  "practicedQuestions": 35,
  "correctAnswers": 28,
  "wrongAnswers": 7,
  "progressPercentage": 70.0,
  "accuracyPercentage": 80.0
}
```

---

### 9. Get Subject Practice Progress
**GET** `/students/{studentId}/practiceProgress/subject?subjectId={subjectId}`

Retrieves practice progress for a specific subject.

#### Path Parameters
- `studentId` (required): The student ID

#### Query Parameters
- `subjectId` (required): The subject ID

#### Response (200 OK)
```json
{
  "studentId": 1,
  "subjectId": "LC5H01",
  "subjectName": "Mathematics",
  "totalQuestions": 200,
  "practicedQuestions": 120,
  "correctAnswers": 95,
  "wrongAnswers": 25,
  "progressPercentage": 60.0,
  "accuracyPercentage": 79.2
}
```

---

### 10. Get Chapter Test Progress
**GET** `/students/{studentId}/testProgress/chapter?chapterId={chapterId}`

Retrieves test progress for a specific chapter.

#### Path Parameters
- `studentId` (required): The student ID

#### Query Parameters
- `chapterId` (required): The chapter ID

#### Response (200 OK)
```json
{
  "studentId": 1,
  "chapterId": "LC5H0101",
  "chapterName": "Number Systems",
  "totalQuestions": 50,
  "testedQuestions": 40,
  "correctAnswers": 35,
  "wrongAnswers": 5,
  "progressPercentage": 80.0,
  "accuracyPercentage": 87.5
}
```

---

### 11. Get Subject Test Progress
**GET** `/students/{studentId}/testProgress/subject?subjectId={subjectId}`

Retrieves test progress for a specific subject.

#### Path Parameters
- `studentId` (required): The student ID

#### Query Parameters
- `subjectId` (required): The subject ID

#### Response (200 OK)
```json
{
  "studentId": 1,
  "subjectId": "LC5H01",
  "subjectName": "Mathematics",
  "totalQuestions": 200,
  "testedQuestions": 150,
  "correctAnswers": 130,
  "wrongAnswers": 20,
  "progressPercentage": 75.0,
  "accuracyPercentage": 86.7
}
```

---

### 12. Get Student Subjects
**GET** `/students/{studentId}/subjects`

Retrieves all subjects selected by a student.

#### Path Parameters
- `studentId` (required): The student ID

#### Response (200 OK)
```json
[
  {
    "subjectId": "LC5H01",
    "subjectName": "Mathematics",
    "classId": 5,
    "className": "Grade 5",
    "selectedAt": "2024-01-15T10:30:00Z"
  },
  {
    "subjectId": "LC5H02",
    "subjectName": "Science",
    "classId": 5,
    "className": "Grade 5",
    "selectedAt": "2024-01-15T10:30:00Z"
  }
]
```

---

### 13. Select Subjects for Student
**POST** `/students/{studentId}/subjects`

Adds subjects to a student's selection (appends to existing).

#### Path Parameters
- `studentId` (required): The student ID

#### Request Body
```json
["LC5H03", "LC5H04"]
```

#### Response (200 OK)
```json
[
  {
    "id": 1,
    "studentId": 1,
    "subjectId": "LC5H03",
    "selectedAt": "2024-01-20T14:30:00Z"
  },
  {
    "id": 2,
    "studentId": 1,
    "subjectId": "LC5H04",
    "selectedAt": "2024-01-20T14:30:00Z"
  }
]
```

---

## Data Models

### Student
```json
{
  "id": "long (unique identifier)",
  "userId": "long (reference to User)",
  "classId": "integer (reference to Class)",
  "name": "string",
  "email": "string"
}
```

### StudentProfileDTO
```json
{
  "studentId": "long",
  "userId": "long",
  "name": "string",
  "email": "string",
  "classId": "integer",
  "className": "string",
  "hasSelectedSubjects": "boolean",
  "selectedSubjectsCount": "integer",
  "isOnboardingComplete": "boolean"
}
```

### StudentPracticeProgressDTO
```json
{
  "studentId": "long",
  "chapterId": "string (optional)",
  "chapterName": "string (optional)",
  "subjectId": "string (optional)",
  "subjectName": "string (optional)",
  "totalQuestions": "integer",
  "practicedQuestions": "integer",
  "correctAnswers": "integer",
  "wrongAnswers": "integer",
  "progressPercentage": "double",
  "accuracyPercentage": "double"
}
```

### StudentSelectedSubjectDTO
```json
{
  "subjectId": "string",
  "subjectName": "string",
  "classId": "integer",
  "className": "string",
  "selectedAt": "datetime (ISO 8601)"
}
```

---

## Notes for UI Team

1. **Student Onboarding Flow**:
   - Check profile with `/students/{studentId}/profile`
   - If no class: Use `/students/{studentId}/class` to set class
   - If no subjects: Use `/students/{studentId}/subjects` (POST) to select subjects

2. **Subject Management**:
   - **POST** `/students/{studentId}/subjects`: Adds new subjects (keeps existing)
   - **PUT** `/students/{studentId}/subjects`: Replaces all subjects with new list
   - Use PUT for "reset and select new" scenarios
   - Use POST for "add more subjects" scenarios

3. **Progress Tracking**:
   - Practice progress: Shows questions practiced in practice mode
   - Test progress: Shows questions attempted in test/exam mode
   - Both provide percentage completion and accuracy metrics

4. **Progress Display**:
   - Use chapter-level progress for detailed chapter views
   - Use subject-level progress for subject overview dashboards
   - Show progress bars with `progressPercentage`
   - Show accuracy indicators with `accuracyPercentage`

5. **Dashboard Widgets**:
   - Overall progress: Aggregate subject-level data
   - Chapter progress: Use for chapter-specific views
   - Wrong answers: Track `wrongAnswers` to suggest review

6. **Error Handling**:
   - Handle 404 when student not found
   - Validate class and subject IDs before submission
   - Show user-friendly messages for validation errors

7. **Performance Considerations**:
   - Progress calculations may be expensive for large datasets
   - Consider caching progress data on the client side
   - Refresh progress data after practice/test sessions

8. **Student ID vs User ID**:
   - `studentId`: Internal student record ID
   - `userId`: Reference to the user account
   - Most endpoints use `studentId`