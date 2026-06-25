# Chapter API Specification

## Base URL
`/chapters`

## Overview
Manages chapters within subjects, including CRUD operations and bulk uploads.

---

## Endpoints

### 1. Get All Chapters
**GET** `/chapters`

Retrieves all chapters or filters by subject.

#### Query Parameters
- `subjectId` (optional): Filter chapters by subject ID

#### Response (200 OK)
```json
[
  {
    "chapterId": "LC5H0101",
    "chapterName": "Number Systems",
    "chapterNumber": 1,
    "subject": {
      "subjectId": "LC5H01",
      "subjectName": "Mathematics",
      "classId": 5
    }
  },
  {
    "chapterId": "LC5H0102",
    "chapterName": "Algebra Basics",
    "chapterNumber": 2,
    "subject": {
      "subjectId": "LC5H01",
      "subjectName": "Mathematics",
      "classId": 5
    }
  }
]
```

#### Example Usage
- Get all chapters: `GET /chapters`
- Get chapters for a subject: `GET /chapters?subjectId=LC5H01`

---

### 2. Create Chapter
**POST** `/chapters`

Creates a single chapter.

#### Request Body
```json
{
  "chapterId": "LC5H0103",
  "chapterName": "Geometry",
  "chapterNumber": 3,
  "subject": {
    "subjectId": "LC5H01"
  }
}
```

#### Response (200 OK)
```json
{
  "chapterId": "LC5H0103",
  "chapterName": "Geometry",
  "chapterNumber": 3,
  "subject": {
    "subjectId": "LC5H01",
    "subjectName": "Mathematics",
    "classId": 5
  }
}
```

---

### 3. Create Multiple Chapters for Subject
**POST** `/chapters/subject/{subjectId}`

Creates multiple chapters for a specific subject.

#### Path Parameters
- `subjectId` (required): The subject ID

#### Request Body
```json
[
  {
    "chapterId": "LC5H0104",
    "chapterName": "Trigonometry",
    "chapterNumber": 4
  },
  {
    "chapterId": "LC5H0105",
    "chapterName": "Statistics",
    "chapterNumber": 5
  }
]
```

#### Response (200 OK)
```json
[
  {
    "chapterId": "LC5H0104",
    "chapterName": "Trigonometry",
    "chapterNumber": 4,
    "subject": {
      "subjectId": "LC5H01",
      "subjectName": "Mathematics",
      "classId": 5
    }
  },
  {
    "chapterId": "LC5H0105",
    "chapterName": "Statistics",
    "chapterNumber": 5,
    "subject": {
      "subjectId": "LC5H01",
      "subjectName": "Mathematics",
      "classId": 5
    }
  }
]
```

---

### 4. Update Chapter
**PUT** `/chapters/{chapterId}`

Updates an existing chapter.

#### Path Parameters
- `chapterId` (required): The chapter ID to update

#### Request Body
```json
{
  "chapterName": "Advanced Geometry",
  "chapterNumber": 3,
  "subject": {
    "subjectId": "LC5H01"
  }
}
```

#### Response (200 OK)
```json
{
  "chapterId": "LC5H0103",
  "chapterName": "Advanced Geometry",
  "chapterNumber": 3,
  "subject": {
    "subjectId": "LC5H01",
    "subjectName": "Mathematics",
    "classId": 5
  }
}
```

---

### 5. Delete Chapter
**DELETE** `/chapters/{chapterId}`

Deletes a specific chapter.

#### Path Parameters
- `chapterId` (required): The chapter ID to delete

#### Response (200 OK)
```json
"Chapter with ID LC5H0103 has been deleted."
```

---

### 6. Delete Chapters by Subject
**DELETE** `/chapters?subjectId={subjectId}`

Deletes all chapters for a specific subject.

#### Query Parameters
- `subjectId` (required): The subject ID

#### Response (200 OK)
```json
"Chapters with SubjectID LC5H01 has been deleted."
```

---

### 7. Upload Chapters from Excel
**POST** `/chapters/upload`

Bulk upload chapters from an Excel file.

#### Request
- **Content-Type**: `multipart/form-data`
- **Form Data**:
  - `file` (required): Excel file containing chapter data

#### Excel File Format
The Excel file should contain the following columns:
- `chapterId`: Unique chapter identifier
- `chapterName`: Name of the chapter
- `chapterNumber`: Sequential number
- `subjectId`: Associated subject ID

#### Response (200 OK)
```json
"Chapters uploaded successfully. Total: 25"
```

#### Error Responses
- **400 Bad Request**: Invalid file format or missing required columns
- **500 Internal Server Error**: File processing error

---

## Data Models

### Chapter
```json
{
  "chapterId": "string (required, unique identifier)",
  "chapterName": "string (required)",
  "chapterNumber": "integer (required)",
  "subject": {
    "subjectId": "string (required)",
    "subjectName": "string",
    "classId": "integer"
  }
}
```

### Subject (Nested)
```json
{
  "subjectId": "string (required)",
  "subjectName": "string",
  "classId": "integer"
}
```

---

## Notes for UI Team

1. **Chapter ID Format**: Chapter IDs typically follow a pattern like `LC5H0101` where:
   - `LC` = Level Code
   - `5` = Class number
   - `H` = Subject code
   - `01` = Subject sequence
   - `01` = Chapter number

2. **Filtering**: Use the `subjectId` query parameter to get chapters for a specific subject when building subject-specific views.

3. **Bulk Operations**: 
   - Use the `/chapters/subject/{subjectId}` endpoint when creating multiple chapters at once
   - Use the Excel upload feature for initial data setup or bulk imports

4. **Delete Operations**:
   - Single chapter deletion: Use `/chapters/{chapterId}`
   - Bulk deletion by subject: Use `/chapters?subjectId={subjectId}`

5. **Subject Association**: When creating chapters, you only need to provide the `subjectId` in the subject object. The full subject details will be returned in the response.

6. **Error Handling**: 
   - Handle 404 errors when chapter or subject is not found
   - Handle 400 errors for validation failures
   - Provide user-friendly messages for Excel upload errors

7. **Excel Upload**: Provide a template Excel file for users to download and fill in. Validate file format before upload.