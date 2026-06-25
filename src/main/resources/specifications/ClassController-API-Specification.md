# Class API Specification

## Base URL
`/classes`

## Overview
Manages class/grade levels in the system (e.g., Grade 5, Grade 6, etc.).

---

## Endpoints

### 1. Get All Classes
**GET** `/classes`

Retrieves all available classes.

#### Response (200 OK)
```json
[
  {
    "classId": 5,
    "className": "Grade 5",
    "description": "Fifth Grade"
  },
  {
    "classId": 6,
    "className": "Grade 6",
    "description": "Sixth Grade"
  },
  {
    "classId": 7,
    "className": "Grade 7",
    "description": "Seventh Grade"
  }
]
```

---

### 2. Create Class
**POST** `/classes`

Creates a new class.

#### Request Body
```json
{
  "classId": 8,
  "className": "Grade 8",
  "description": "Eighth Grade"
}
```

#### Response (200 OK)
```json
{
  "classId": 8,
  "className": "Grade 8",
  "description": "Eighth Grade"
}
```

---

### 3. Update Class
**PUT** `/classes/{classId}`

Updates an existing class.

#### Path Parameters
- `classId` (required): The class ID to update

#### Request Body
```json
{
  "className": "Grade 8 Advanced",
  "description": "Eighth Grade - Advanced Track"
}
```

#### Response (200 OK)
```json
{
  "classId": 8,
  "className": "Grade 8 Advanced",
  "description": "Eighth Grade - Advanced Track"
}
```

---

### 4. Delete Class
**DELETE** `/classes/{classId}`

Deletes a specific class.

#### Path Parameters
- `classId` (required): The class ID to delete

#### Response (204 No Content)
```
(Empty response body)
```

#### Error Responses
- **404 Not Found**: Class not found
- **409 Conflict**: Cannot delete class with associated subjects or students

---

## Data Models

### ClassEntity
```json
{
  "classId": "integer (required, unique)",
  "className": "string (required)",
  "description": "string (optional)"
}
```

---

## Notes for UI Team

1. **Class ID**: Typically represents the grade level (5, 6, 7, 8, etc.)

2. **Class Name**: Display-friendly name (e.g., "Grade 5", "Fifth Grade")

3. **Dropdown Lists**: Use this endpoint to populate class selection dropdowns in:
   - Student registration forms
   - Subject assignment forms
   - Filtering options

4. **Delete Restrictions**: 
   - Cannot delete a class if it has associated subjects
   - Cannot delete a class if students are enrolled in it
   - Show appropriate error messages to users

5. **Sorting**: Classes are typically displayed in ascending order by classId

6. **Common Use Cases**:
   - Student profile: Display and update student's class
   - Subject management: Filter subjects by class
   - Teacher assignment: Assign teachers to specific classes

7. **Error Handling**: Handle 404 and 409 errors gracefully with user-friendly messages