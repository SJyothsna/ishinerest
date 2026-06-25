# Question API Specification

## Base URL
`/questions`

## Overview
Comprehensive question management including system questions, custom questions, practice questions, and flagged questions. Supports filtering, bulk operations, and Excel uploads.

---

## Endpoints

### Basic CRUD Operations

#### 1. Get All Questions
**GET** `/questions?createdBy={userId}`

Retrieves questions created by a specific user or system questions.

**Query Parameters:**
- `createdBy` (optional): User ID of question creator

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "sectionId": "LC5H0101-S1",
    "questionText": "What is 2 + 2?",
    "optionA": "3",
    "optionB": "4",
    "optionC": "5",
    "optionD": "6",
    "correctAnswer": "B",
    "questionType": "SINGLE_CHOICE",
    "difficultyLevel": "EASY",
    "usageType": "PRACTICE",
    "isCustom": false,
    "createdBy": null,
    "questionImageUrl": null
  }
]
```

---

#### 2. Get Question by ID
**GET** `/questions/{id}`

**Response (200 OK):** Single question object

---

#### 3. Get Questions by Chapter
**GET** `/questions/chapter/{chapterId}`

**Response (200 OK):** Array of questions for the chapter

---

#### 4. Get Questions by Subject
**GET** `/questions/subject/{subjectId}`

**Response (200 OK):** Array of questions for the subject

---

#### 5. Create Questions
**POST** `/questions`

Creates multiple questions at once.

**Request Body:**
```json
[
  {
    "sectionId": "LC5H0101-S1",
    "questionText": "What is 3 + 3?",
    "optionA": "5",
    "optionB": "6",
    "optionC": "7",
    "optionD": "8",
    "correctAnswer": "B",
    "questionType": "SINGLE_CHOICE",
    "difficultyLevel": "EASY",
    "usageType": "PRACTICE"
  }
]
```

**Response (200 OK):** Array of created questions

---

#### 6. Upload Questions from Excel
**POST** `/questions/upload`

Bulk upload questions from Excel file.

**Request:** `multipart/form-data` with `file` parameter

**Response (200 OK):** Array of created questions

---

#### 7. Update Question
**PUT** `/questions/{id}`

Updates an existing question.

**Request Body:** Complete question object

**Response (200 OK):** Updated question

---

#### 8. Delete Question
**DELETE** `/questions/{id}`

**Response (200 OK):** Empty

---

### Practice Questions (Unpracticed)

#### 9. Get Unpracticed Questions by Subject
**GET** `/questions/unpracticed/subject?studentId={id}&subjectId={id}&limit={n}&usageType={type}&questionSet={set}`

**Query Parameters:**
- `studentId` (required): Student ID
- `subjectId` (required): Subject ID
- `limit` (optional, default=10): Number of questions
- `usageType` (optional): PRACTICE, TEST, or EXAM
- `questionSet` (optional): Question set number (1, 2, 3, etc.) for progressive practice

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "sectionId": "LC5H0101-S1",
    "questionText": "What is 2 + 2?",
    "optionA": "3",
    "optionB": "4",
    "optionC": "5",
    "optionD": "6",
    "correctAnswer": "B",
    "isFlagged": false
  }
]
```

---

#### 10. Get Unpracticed Questions by Chapter
**GET** `/questions/unpracticed/chapter?studentId={id}&chapterId={id}&limit={n}&level={level}&usageType={type}&sectionId={id}&questionSet={set}`

**Query Parameters:**
- `studentId` (required): Student ID
- `chapterId` (required): Chapter ID
- `limit` (optional, default=10): Number of questions
- `level` (optional): Difficulty level (EASY, MEDIUM, HARD)
- `usageType` (optional): PRACTICE, TEST, or EXAM
- `sectionId` (optional): Section ID for filtering
- `questionSet` (optional): Question set number (1, 2, 3, etc.) for progressive practice

**Response (200 OK):** Array of QuestionWithFlagDTO

---

### Questions with Flag Status

#### 11. Get Chapter Questions with Flags
**GET** `/questions/chapter/{chapterId}/with-flags?studentId={id}`

Returns all questions for a chapter with flag status for the student.

**Response (200 OK):** Array of QuestionWithFlagDTO

---

#### 12. Get Subject Questions with Flags
**GET** `/questions/subject/{subjectId}/with-flags?studentId={id}`

**Response (200 OK):** Array of QuestionWithFlagDTO

---

### Wrong/Unpracticed Questions

#### 13. Get Wrong Unpracticed Questions by Chapter
**GET** `/questions/wrong-unpracticed/chapter?studentId={id}&chapterId={id}`

Returns questions the student answered incorrectly and hasn't correctly answered yet.

**Response (200 OK):** Array of QuestionWithFlagDTO

---

#### 14. Get Wrong Unpracticed Questions by Subject
**GET** `/questions/wrong-unpracticed/subject?studentId={id}&subjectId={id}`

**Response (200 OK):** Array of QuestionWithFlagDTO

---

### Custom Questions (Teachers/Parents)

#### 15. Create Custom Question
**POST** `/questions/custom?creatorUserId={id}`

Creates a custom question by a teacher or parent.

**Query Parameters:**
- `creatorUserId` (required): User ID of creator

**Request Body:** Question object

**Response (200 OK):** Created question with `isCustom=true`

---

#### 16. Get Custom Questions by Creator
**GET** `/questions/custom/creator/{creatorUserId}`

**Response (200 OK):** Array of custom questions

---

#### 17. Get Custom Questions by Creator and Chapter
**GET** `/questions/custom/creator/{creatorUserId}/chapter/{chapterId}`

**Response (200 OK):** Array of custom questions for specific chapter

---

#### 18. Update Question Visibility
**PUT** `/questions/{questionId}/visibility?creatorUserId={id}&visibility={status}`

Updates visibility of a custom question.

**Query Parameters:**
- `creatorUserId` (required): Creator's user ID
- `visibility` (required): PUBLIC, PRIVATE, or SHARED

**Response (200 OK):** Updated question

---

#### 19. Delete Custom Question
**DELETE** `/questions/custom/{questionId}?creatorUserId={id}`

**Query Parameters:**
- `creatorUserId` (required): Creator's user ID (for authorization)

**Response (200 OK):** Empty

---

#### 20. Get System Questions
**GET** `/questions/system`

Returns all non-custom (system) questions.

**Response (200 OK):** Array of system questions

---

#### 21. Get All Custom Questions (Admin)
**GET** `/questions/all-custom`

Returns all custom questions (admin only).

**Response (200 OK):** Array of all custom questions

---

## Data Models

### Question
```json
{
  "id": "long",
  "sectionId": "string (chapter-section identifier)",
  "questionText": "string (nullable for image-only questions)",
  "optionA": "string",
  "optionB": "string",
  "optionC": "string",
  "optionD": "string",
  "optionE": "string (optional)",
  "optionF": "string (optional)",
  "correctAnswer": "string (A, B, C, D, E, F)",
  "correctAnswers": "string (comma-separated for multi-choice)",
  "questionType": "enum (SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE)",
  "difficultyLevel": "enum (EASY, MEDIUM, HARD)",
  "explanation": "string (optional)",
  "notes": "string (optional)",
  "hint": "string (optional)",
  "usageType": "enum (PRACTICE, TEST, EXAM)",
  "questionSet": "string (default: '1', for progressive practice sets)",
  "questionImageUrl": "string (optional)",
  "isCustom": "boolean",
  "createdBy": "long (nullable for system questions)"
}
```

### QuestionWithFlagDTO
```json
{
  "id": "long",
  "sectionId": "string",
  "questionText": "string",
  "optionA": "string",
  "optionB": "string",
  "optionC": "string",
  "optionD": "string",
  "optionE": "string",
  "optionF": "string",
  "correctAnswer": "string",
  "correctAnswers": "string",
  "questionType": "string",
  "difficultyLevel": "string",
  "explanation": "string",
  "hint": "string",
  "usageType": "string",
  "questionImageUrl": "string",
  "isFlagged": "boolean"
}
```

---

## Notes for UI Team

### Question Types
- **SINGLE_CHOICE**: One correct answer (A, B, C, or D)
- **MULTIPLE_CHOICE**: Multiple correct answers (use `correctAnswers` field)
- **TRUE_FALSE**: Two options only

### Usage Types
- **PRACTICE**: For practice sessions
- **TEST**: For teacher-created tests
- **EXAM**: For official exams

### Difficulty Levels
- **EASY**: Beginner level
- **MEDIUM**: Intermediate level
- **HARD**: Advanced level

### Practice Mode Features
1. Use unpracticed endpoints to get fresh questions
2. Filter by difficulty level for adaptive learning
3. Show flag status to indicate bookmarked questions
4. Track wrong answers for review sessions

### Custom Questions
1. Teachers and parents can create custom questions
2. Custom questions are linked to creator
3. Visibility controls: PUBLIC, PRIVATE, SHARED
4. Can be assigned to specific students

### Image Support
- Questions can have images via `questionImageUrl`
- `questionText` can be null for image-only questions
- Use QuestionImageController for image uploads

### Flag Integration
- Questions can be flagged by students for review
- `isFlagged` indicates if current student flagged it
- Use FlaggedQuestionController to manage flags

### Wrong Answer Review
- Track questions answered incorrectly
- Use wrong-unpracticed endpoints for review sessions
- Help students focus on weak areas

### Excel Upload
- Provide template for bulk question upload
- Validate format before upload
- Show progress during upload

### Performance Tips
- Use limit parameter to control question count
- Cache questions client-side during practice
- Prefetch next batch of questions

### Error Handling
- Handle 404 for missing questions
- Validate question format before submission
- Show user-friendly messages for validation errors