# Subject API Specification

## Base URL
`/subjects`

## Overview
Manages subjects/courses within classes (e.g., Mathematics, Science, English).

---

## Endpoints

### 1. Get All Subjects
**GET** `/subjects`

Retrieves all available subjects.

#### Response (200 OK)
```json
[
  {
    "subjectId": "LC5H01",
    "subjectName": "Mathematics",
    "classId": 5,
    "description": "Fifth Grade Mathematics"
  },
  {
    "subjectId": "LC5H02",
    "subjectName": "Science",
    "classId": 5,
    "description": "Fifth Grade Science"
  },
  {
    "subjectId": "LC6H01",
    "subjectName": "Mathematics",
    "classId": 6,
    "description": "Sixth Grade Mathematics"
  }
]
```

---

### 2. Get Subjects by Class
**GET** `/subjects/class/{classId}`

Retrieves all subjects for a specific class.

#### Path Parameters
- `classId` (required): The class ID

#### Response (200 OK)
```json
[
  {
    "subjectId": "LC5H01",
    "subjectName": "Mathematics",
    "classId": 5,
    "description": "Fifth Grade Mathematics"
  },
  {
    "subjectId": "LC5H02",
    "subjectName": "Science",
    "classId": 5,
    "description": "Fifth Grade Science"
  },
  {
    "subjectId": "LC5H03",
    "subjectName": "English",
    "classId": 5,
    "description": "Fifth Grade English"
  }
]
```

---

### 3. Create Multiple Subjects
**POST** `/subjects`

Creates multiple subjects at once.

#### Request Body
```json
[
  {
    "subjectId": "LC5H04",
    "subjectName": "History",
    "classId": 5,
    "description": "Fifth Grade History"
  },
  {
    "subjectId": "LC5H05",
    "subjectName": "Geography",
    "classId": 5,
    "description": "Fifth Grade Geography"
  }
]
```

#### Response (200 OK)
```json
[
  {
    "subjectId": "LC5H04",
    "subjectName": "History",
    "classId": 5,
    "description": "Fifth Grade History"
  },
  {
    "subjectId": "LC5H05",
    "subjectName": "Geography",
    "classId": 5,
    "description": "Fifth Grade Geography"
  }
]
```

---

### 4. Update Subject
**PUT** `/subjects/{subjectId}`

Updates an existing subject.

#### Path Parameters
- `subjectId` (required): The subject ID to update

#### Request Body
```json
{
  "subjectName": "Advanced Mathematics",
  "classId": 5,
  "description": "Fifth Grade Advanced Mathematics"
}
```

#### Response (200 OK)
```json
{
  "subjectId": "LC5H01",
  "subjectName": "Advanced Mathematics",
  "classId": 5,
  "description": "Fifth Grade Advanced Mathematics"
}
```

---

### 5. Delete Subject
**DELETE** `/subjects/{subjectId}`

Deletes a specific subject.

#### Path Parameters
- `subjectId` (required): The subject ID to delete

#### Response (200 OK)
```json
"Subject with ID LC5H01 has been deleted."
```

#### Error Responses
- **404 Not Found**: Subject not found
- **409 Conflict**: Cannot delete subject with associated chapters or questions

---

## Data Models

### SubjectEntity
```json
{
  "subjectId": "string (required, unique identifier)",
  "subjectName": "string (required)",
  "classId": "integer (required)",
  "description": "string (optional)"
}
```

---

## Notes for UI Team

1. **Subject ID Format**: Subject IDs typically follow a pattern like `LC5H01` where:
   - `LC` = Level Code
   - `5` = Class number
   - `H` = Higher level (or other designation)
   - `01` = Subject sequence number

2. **Class Association**: Each subject belongs to a specific class. Use the `/subjects/class/{classId}` endpoint to get subjects for a particular grade level.

3. **Bulk Creation**: The POST endpoint accepts an array, allowing you to create multiple subjects in one request. This is useful for initial setup or bulk imports.

4. **Common Use Cases**:
   - Student enrollment: Display available subjects for a student's class
   - Teacher assignment: Show subjects a teacher can teach
   - Chapter management: Filter chapters by subject
   - Question bank: Organize questions by subject

5. **Dropdown Population**:
   - Use `/subjects/class/{classId}` when you know the student's class
   - Use `/subjects` for admin views or when showing all subjects

6. **Delete Restrictions**:
   - Cannot delete a subject if it has associated chapters
   - Cannot delete a subject if it has questions
   - Cannot delete a subject if students are enrolled in it
   - Show appropriate error messages to users

7. **Filtering and Search**: Consider implementing client-side filtering by subject name for better UX when displaying long lists.

8. **Subject Hierarchy**: 
   - Class → Subject → Chapter → Questions
   - Understanding this hierarchy helps in building navigation and filtering

9. **Error Handling**: Handle 404 and 409 errors gracefully with user-friendly messages