# Teacher API Specification

## Base URL
`/teachers`

## Overview
Manages teacher-student relationships, student enrollment, and teacher subject assignments.

---

## Endpoints

### 1. Link Teacher to Student
**POST** `/teachers/{teacherUserId}/students`

Creates a link between a teacher and an existing student.

#### Path Parameters
- `teacherUserId` (required): The teacher's user ID

#### Request Body
```json
{
  "studentUserId": 15
}
```

#### Response (201 Created)
```
(Empty response body)
```

---

### 2. Add or Link Student by Email
**POST** `/teachers/{teacherUserId}/students/add-or-link`

Adds a student by email. If the email exists, links that student. Otherwise, creates a guest student account.

#### Path Parameters
- `teacherUserId` (required): The teacher's user ID

#### Request Body
```json
{
  "name": "John Doe",
  "email": "john.doe@example.com"
}
```

#### Response (201 Created)
```json
{
  "studentUserId": 15,
  "name": "John Doe",
  "email": "john.doe@example.com",
  "wasExisting": false,
  "message": "New guest student account created and linked"
}
```

#### Notes
- If `wasExisting` is `true`, the student already existed and was linked
- If `wasExisting` is `false`, a new guest account was created

---

### 3. Unlink Teacher from Student
**DELETE** `/teachers/{teacherUserId}/students/{studentUserId}`

Removes the link between a teacher and a student.

#### Path Parameters
- `teacherUserId` (required): The teacher's user ID
- `studentUserId` (required): The student's user ID

#### Response (204 No Content)
```
(Empty response body)
```

---

### 4. Get Students for Teacher
**GET** `/teachers/{teacherUserId}/students`

Retrieves all students linked to a teacher.

#### Path Parameters
- `teacherUserId` (required): The teacher's user ID

#### Response (200 OK)
```json
[
  {
    "userId": 15,
    "name": "John Doe",
    "email": "john.doe@example.com",
    "role": "STUDENT",
    "isActive": true,
    "emailVerified": true,
    "createdAt": "2024-01-15T10:30:00Z"
  },
  {
    "userId": 16,
    "name": "Jane Smith",
    "email": "jane.smith@example.com",
    "role": "STUDENT",
    "isActive": true,
    "emailVerified": true,
    "createdAt": "2024-01-16T09:15:00Z"
  }
]
```

---

### 5. Get Teachers for Student
**GET** `/teachers/students/{studentUserId}/teachers`

Retrieves all teachers linked to a student.

#### Path Parameters
- `studentUserId` (required): The student's user ID

#### Response (200 OK)
```json
[
  {
    "userId": 5,
    "name": "Mr. Anderson",
    "email": "anderson@school.com",
    "role": "TEACHER",
    "isActive": true,
    "emailVerified": true,
    "createdAt": "2024-01-01T08:00:00Z"
  }
]
```

---

### 6. Check if Teacher is Linked to Student
**GET** `/teachers/{teacherUserId}/students/{studentUserId}/linked`

Checks if a teacher is linked to a specific student.

#### Path Parameters
- `teacherUserId` (required): The teacher's user ID
- `studentUserId` (required): The student's user ID

#### Response (200 OK)
```json
true
```

---

### 7. Get Teacher Subjects
**GET** `/teachers/{teacherUserId}/subjects`

Retrieves all subjects selected by a teacher.

#### Path Parameters
- `teacherUserId` (required): The teacher's user ID

#### Response (200 OK)
```json
[
  {
    "subjectId": "LC5H01",
    "subjectName": "Mathematics",
    "classId": 5,
    "className": "Grade 5",
    "selectedAt": "2024-01-10T09:00:00Z"
  },
  {
    "subjectId": "LC6H01",
    "subjectName": "Mathematics",
    "classId": 6,
    "className": "Grade 6",
    "selectedAt": "2024-01-10T09:00:00Z"
  }
]
```

---

### 8. Add Teacher Subjects
**POST** `/teachers/{teacherUserId}/subjects`

Adds subjects to a teacher's selection (appends to existing).

#### Path Parameters
- `teacherUserId` (required): The teacher's user ID

#### Request Body
```json
{
  "subjectIds": ["LC5H02", "LC5H03"]
}
```

#### Response (201 Created)
```json
[
  {
    "subjectId": "LC5H02",
    "subjectName": "Science",
    "classId": 5,
    "className": "Grade 5",
    "selectedAt": "2024-01-20T10:00:00Z"
  },
  {
    "subjectId": "LC5H03",
    "subjectName": "English",
    "classId": 5,
    "className": "Grade 5",
    "selectedAt": "2024-01-20T10:00:00Z"
  }
]
```

---

### 9. Replace Teacher Subjects
**PUT** `/teachers/{teacherUserId}/subjects`

Replaces all subjects for a teacher with a new list.

#### Path Parameters
- `teacherUserId` (required): The teacher's user ID

#### Request Body
```json
{
  "subjectIds": ["LC5H01", "LC6H01"]
}
```

#### Response (204 No Content)
```
(Empty response body)
```

---

### 10. Delete Teacher Subject
**DELETE** `/teachers/{teacherUserId}/subjects/{subjectId}`

Removes a specific subject from a teacher's selection.

#### Path Parameters
- `teacherUserId` (required): The teacher's user ID
- `subjectId` (required): The subject ID to remove

#### Response (204 No Content)
```
(Empty response body)
```

---

## Data Models

### LinkStudentRequest
```json
{
  "studentUserId": "long (required)"
}
```

### AddStudentByEmailRequest
```json
{
  "name": "string (required, 2-100 characters)",
  "email": "string (required, valid email format)"
}
```

### AddStudentByEmailResponse
```json
{
  "studentUserId": "long",
  "name": "string",
  "email": "string",
  "wasExisting": "boolean",
  "message": "string"
}
```

### TeacherSubjectSelectionRequest
```json
{
  "subjectIds": "array of strings (required)"
}
```

### TeacherSelectedSubjectDTO
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

1. **CORS Configuration**: The API allows requests from `http://localhost:3000` and `http://localhost:5173` with credentials.

2. **Student Management**:
   - Use `/teachers/{teacherUserId}/students` (POST) to link existing students
   - Use `/teachers/{teacherUserId}/students/add-or-link` (POST) to add by email
   - The add-or-link endpoint handles both scenarios automatically

3. **Guest Student Accounts**:
   - When adding a student by email that doesn't exist, a guest account is created
   - Guest accounts have limited access until the student verifies their email
   - The `wasExisting` flag indicates if the student was newly created

4. **Subject Management**:
   - **POST** `/teachers/{teacherUserId}/subjects`: Adds new subjects (keeps existing)
   - **PUT** `/teachers/{teacherUserId}/subjects`: Replaces all subjects
   - **DELETE** `/teachers/{teacherUserId}/subjects/{subjectId}`: Removes one subject
   - Use PUT for "reset and select new" scenarios
   - Use POST for "add more subjects" scenarios

5. **Teacher Dashboard**:
   - Use `/teachers/{teacherUserId}/students` to display student list
   - Use `/teachers/{teacherUserId}/subjects` to show teaching subjects
   - Show student count and subject count as metrics

6. **Student View**:
   - Use `/teachers/students/{studentUserId}/teachers` to show all teachers for a student
   - Useful for student dashboards showing their teachers

7. **Relationship Checks**:
   - Use `/teachers/{teacherUserId}/students/{studentUserId}/linked` to verify relationships
   - Useful before allowing access to student data

8. **Multi-Class Teaching**:
   - Teachers can select subjects from multiple classes
   - Display subjects grouped by class for better organization
   - Example: "Grade 5 Mathematics", "Grade 6 Mathematics"

9. **Error Handling**:
   - Handle 404 when teacher or student not found
   - Handle 409 when trying to create duplicate links
   - Handle 400 for invalid email formats
   - Provide user-friendly error messages

10. **Security Considerations**:
    - Only allow teachers to manage their own student links
    - Verify teacher permissions before showing student data
    - Log all link/unlink operations for audit purposes