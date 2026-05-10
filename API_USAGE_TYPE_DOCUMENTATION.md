# Question API with usage_type Field

## Overview
The `usage_type` field has been added to all question-related endpoints. This field indicates where a question can be used:
- **"Practice"** - Question available only for practice sessions
- **"Test"** - Question available only for tests/exams
- **"Both"** - Question available for both practice and tests (default)

---

## API Endpoints

### 1. GET All Questions
**Endpoint:** `GET /questions`

**Response Example:**
```json
[
  {
    "questionId": 1,
    "questionText": "What is 2 + 2?",
    "optionA": "3",
    "optionB": "4",
    "optionC": "5",
    "optionD": "6",
    "optionE": null,
    "optionF": null,
    "correctAnswer": "B",
    "correctAnswers": null,
    "questionType": 1,
    "explanation": "Basic addition",
    "difficultyLevel": "Basic",
    "notes": "Simple math question",
    "createdBy": null,
    "tags": null,
    "sectionId": null,
    "usageType": "Both"
  }
]
```

---

### 2. GET Question by ID
**Endpoint:** `GET /questions/{id}`

**Response Example:**
```json
{
  "questionId": 1,
  "questionText": "What is 2 + 2?",
  "optionA": "3",
  "optionB": "4",
  "optionC": "5",
  "optionD": "6",
  "optionE": null,
  "optionF": null,
  "correctAnswer": "B",
  "correctAnswers": null,
  "questionType": 1,
  "explanation": "Basic addition",
  "difficultyLevel": "Basic",
  "notes": "Simple math question",
  "createdBy": null,
  "tags": null,
  "sectionId": null,
  "usageType": "Practice"
}
```

---

### 3. GET Questions by Chapter
**Endpoint:** `GET /questions/chapter/{chapterId}`

**Response Example:**
```json
[
  {
    "questionId": 5,
    "questionText": "Solve for x: 2x + 5 = 15",
    "optionA": "5",
    "optionB": "10",
    "optionC": "15",
    "optionD": "20",
    "optionE": null,
    "optionF": null,
    "correctAnswer": "A",
    "correctAnswers": null,
    "questionType": 1,
    "explanation": "Subtract 5 from both sides, then divide by 2",
    "difficultyLevel": "Intermediate",
    "notes": null,
    "createdBy": null,
    "tags": null,
    "sectionId": null,
    "usageType": "Test"
  }
]
```

---

### 4. GET Questions by Subject
**Endpoint:** `GET /questions/subject/{subjectId}`

**Response Example:**
```json
[
  {
    "questionId": 10,
    "questionText": "What is the capital of France?",
    "optionA": "London",
    "optionB": "Paris",
    "optionC": "Berlin",
    "optionD": "Madrid",
    "optionE": null,
    "optionF": null,
    "correctAnswer": "B",
    "correctAnswers": null,
    "questionType": 1,
    "explanation": "Paris is the capital and largest city of France",
    "difficultyLevel": "Basic",
    "notes": null,
    "createdBy": null,
    "tags": null,
    "sectionId": null,
    "usageType": "Both"
  }
]
```

---

### 5. POST Create Questions
**Endpoint:** `POST /questions`

**Request Body:**
```json
[
  {
    "questionText": "What is the square root of 144?",
    "optionA": "10",
    "optionB": "11",
    "optionC": "12",
    "optionD": "13",
    "optionE": null,
    "optionF": null,
    "correctAnswer": "C",
    "correctAnswers": null,
    "questionType": 1,
    "explanation": "12 × 12 = 144",
    "difficultyLevel": "Basic",
    "notes": "Square root calculation",
    "usageType": "Practice",
    "chapter": {
      "chapterId": "LC5H0102"
    }
  }
]
```

**Response:**
```json
[
  {
    "questionId": 25,
    "questionText": "What is the square root of 144?",
    "optionA": "10",
    "optionB": "11",
    "optionC": "12",
    "optionD": "13",
    "optionE": null,
    "optionF": null,
    "correctAnswer": "C",
    "correctAnswers": null,
    "questionType": 1,
    "explanation": "12 × 12 = 144",
    "difficultyLevel": "Basic",
    "notes": "Square root calculation",
    "createdBy": null,
    "tags": null,
    "sectionId": null,
    "usageType": "Practice"
  }
]
```

---

### 6. PUT Update Question
**Endpoint:** `PUT /questions/{id}`

**Request Body:**
```json
{
  "questionText": "What is the square root of 144?",
  "optionA": "10",
  "optionB": "11",
  "optionC": "12",
  "optionD": "13",
  "optionE": null,
  "optionF": null,
  "correctAnswer": "C",
  "correctAnswers": null,
  "questionType": 1,
  "explanation": "12 × 12 = 144, therefore √144 = 12",
  "difficultyLevel": "Intermediate",
  "notes": "Updated explanation",
  "usageType": "Test",
  "chapter": {
    "chapterId": "LC5H0102"
  }
}
```

**Response:**
```json
{
  "questionId": 25,
  "questionText": "What is the square root of 144?",
  "optionA": "10",
  "optionB": "11",
  "optionC": "12",
  "optionD": "13",
  "optionE": null,
  "optionF": null,
  "correctAnswer": "C",
  "correctAnswers": null,
  "questionType": 1,
  "explanation": "12 × 12 = 144, therefore √144 = 12",
  "difficultyLevel": "Intermediate",
  "notes": "Updated explanation",
  "createdBy": null,
  "tags": null,
  "sectionId": null,
  "usageType": "Test"
}
```

---

### 7. GET Unpracticed Questions by Subject (with flags)
**Endpoint:** `GET /questions/unpracticed/subject?studentId={studentId}&subjectId={subjectId}&limit={limit}&usageType={usageType}`

**Query Parameters:**
- `studentId` (required): The student ID
- `subjectId` (required): The subject ID
- `limit` (optional, default=10): Maximum number of questions to return
- `usageType` (optional): Filter by usage type - "Practice", "Test", or "Both". If not provided or "all", returns all questions regardless of usage type.

**Example Request:**
```
GET /questions/unpracticed/subject?studentId=4&subjectId=LC5H01&limit=5&usageType=Test
```

**Response Example:**
```json
[
  {
    "question": {
      "questionId": 30,
      "questionText": "What is photosynthesis?",
      "optionA": "Process of making food",
      "optionB": "Process of breathing",
      "optionC": "Process of digestion",
      "optionD": "Process of reproduction",
      "optionE": null,
      "optionF": null,
      "correctAnswer": "A",
      "correctAnswers": null,
      "questionType": 1,
      "explanation": "Photosynthesis is the process by which plants make their own food",
      "difficultyLevel": "Basic",
      "notes": null,
      "createdBy": null,
      "tags": null,
      "sectionId": null,
      "usageType": "Practice"
    },
    "isFlagged": false
  }
]
```

---

### 8. GET Unpracticed Questions by Chapter (with flags)
**Endpoint:** `GET /questions/unpracticed/chapter?studentId={studentId}&chapterId={chapterId}&limit={limit}&level={level}&usageType={usageType}`

**Query Parameters:**
- `studentId` (required): The student ID
- `chapterId` (required): The chapter ID
- `limit` (optional, default=10): Maximum number of questions to return
- `level` (optional): Filter by difficulty level - "Basic", "Intermediate", "Advanced", or "all" for all levels
- `usageType` (optional): Filter by usage type - "Practice", "Test", or "Both". If not provided or "all", returns all questions regardless of usage type.

**Example Request:**
```
GET /questions/unpracticed/chapter?studentId=4&chapterId=LC5H0102&limit=5&level=Basic&usageType=Practice
```

**Response Example:**
```json
[
  {
    "question": {
      "questionId": 35,
      "questionText": "Calculate the derivative of x²",
      "optionA": "x",
      "optionB": "2x",
      "optionC": "x²",
      "optionD": "2x²",
      "optionE": null,
      "optionF": null,
      "correctAnswer": "B",
      "correctAnswers": null,
      "questionType": 1,
      "explanation": "Using power rule: d/dx(x²) = 2x",
      "difficultyLevel": "Intermediate",
      "notes": null,
      "createdBy": null,
      "tags": null,
      "sectionId": null,
      "usageType": "Both"
    },
    "isFlagged": true
  }
]
```

---

### 9. GET Questions by Chapter with Flags
**Endpoint:** `GET /questions/chapter/{chapterId}/with-flags?studentId={studentId}`

**Response Example:**
```json
[
  {
    "question": {
      "questionId": 40,
      "questionText": "What is the Pythagorean theorem?",
      "optionA": "a + b = c",
      "optionB": "a² + b² = c²",
      "optionC": "a × b = c",
      "optionD": "a² - b² = c²",
      "optionE": null,
      "optionF": null,
      "correctAnswer": "B",
      "correctAnswers": null,
      "questionType": 1,
      "explanation": "In a right triangle, the square of the hypotenuse equals the sum of squares of the other two sides",
      "difficultyLevel": "Basic",
      "notes": null,
      "createdBy": null,
      "tags": null,
      "sectionId": null,
      "usageType": "Test"
    },
    "isFlagged": false
  }
]
```

---

### 10. GET Wrong Unpracticed Questions by Chapter
**Endpoint:** `GET /questions/wrong-unpracticed/chapter?studentId={studentId}&chapterId={chapterId}`

**Response Example:**
```json
[
  {
    "question": {
      "questionId": 45,
      "questionText": "What is the formula for area of a circle?",
      "optionA": "πr",
      "optionB": "2πr",
      "optionC": "πr²",
      "optionD": "2πr²",
      "optionE": null,
      "optionF": null,
      "correctAnswer": "C",
      "correctAnswers": null,
      "questionType": 1,
      "explanation": "Area = π × radius²",
      "difficultyLevel": "Basic",
      "notes": null,
      "createdBy": null,
      "tags": null,
      "sectionId": null,
      "usageType": "Practice"
    },
    "isFlagged": true
  }
]
```

---

## Excel Upload Format

When uploading questions via Excel, the columns should be in this order:

| Column | Field | Example |
|--------|-------|---------|
| 0 | chapterId | LC5H0102 |
| 1 | questionText | What is 2 + 2? |
| 2 | correctAnswer | B |
| 3 | optionA | 3 |
| 4 | optionB | 4 |
| 5 | optionC | 5 |
| 6 | optionD | 6 |
| 7 | optionE | (optional) |
| 8 | optionF | (optional) |
| 9 | explanation | Basic addition |
| 10 | difficultyLevel | Basic |
| 11 | questionType | 1 |
| 12 | correctAnswers | (for multi-select) |
| 13 | notes | Additional notes |
| 14 | **usageType** | **Practice / Test / Both** |

**Note:** If usageType is not provided in Excel, it defaults to "Both"

---

## Usage Type Values

| Value | Description | Use Case |
|-------|-------------|----------|
| Practice | Question only appears in practice sessions | For learning and skill building |
| Test | Question only appears in tests/exams | For formal assessments |
| Both | Question can appear in both contexts | General purpose questions (default) |

---

## Database Schema

```sql
ALTER TABLE questions ADD COLUMN usage_type VARCHAR(20);
UPDATE questions SET usage_type = 'Both' WHERE usage_type IS NULL;
ALTER TABLE questions ADD CONSTRAINT chk_usage_type 
    CHECK (usage_type IN ('Practice', 'Test', 'Both'));