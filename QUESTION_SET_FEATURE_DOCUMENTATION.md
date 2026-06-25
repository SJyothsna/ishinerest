# Question Set Feature Documentation

## Overview

The Question Set feature allows questions to be organized into progressive sets (Set 1, Set 2, Set 3, etc.) to provide students with additional practice material. When students complete all questions in one set, they can move to the next set for more practice with similar questions.

## Database Schema

### New Column: `question_set`

- **Table**: `questions`
- **Column Name**: `question_set`
- **Data Type**: `VARCHAR(20)`
- **Default Value**: `"1"`
- **Nullable**: No
- **Index**: Yes (`idx_questions_question_set`)

### Migration Script

Run the migration script to add the `question_set` column:

```sql
-- File: database_migration_add_question_set.sql
ALTER TABLE questions 
ADD COLUMN question_set VARCHAR(20) DEFAULT '1';

UPDATE questions 
SET question_set = '1' 
WHERE question_set IS NULL OR question_set = '';

CREATE INDEX idx_questions_question_set ON questions(question_set);
```

## Entity Changes

### Question Entity

Added new field to `Question.java`:

```java
@Column(length = 20)
private String questionSet = "1"; // Question set number (1, 2, 3, etc.)
```

## API Endpoints

### 1. Get Unpracticed Questions by Subject (with Question Set)

**Endpoint**: `GET /questions/unpracticed/subject`

**Parameters**:
- `studentId` (required): Student ID
- `subjectId` (required): Subject ID
- `limit` (optional, default=10): Number of questions to return
- `usageType` (optional): Filter by usage type (Practice, Test, Both, or "all")
- `questionSet` (optional): Filter by question set (e.g., "1", "2", "3", or "all")

**Example Requests**:

```http
# Get Set 1 questions
GET /questions/unpracticed/subject?studentId=1&subjectId=MATH&limit=10&questionSet=1

# Get Set 2 questions
GET /questions/unpracticed/subject?studentId=1&subjectId=MATH&limit=10&questionSet=2

# Get all sets (backward compatible)
GET /questions/unpracticed/subject?studentId=1&subjectId=MATH&limit=10

# Get all sets explicitly
GET /questions/unpracticed/subject?studentId=1&subjectId=MATH&limit=10&questionSet=all
```

**Response**:
```json
[
  {
    "questionId": 1,
    "questionText": "What is 2 + 2?",
    "optionA": "3",
    "optionB": "4",
    "optionC": "5",
    "optionD": "6",
    "correctAnswer": "B",
    "questionSet": "1",
    "isFlagged": false
  }
]
```

---

### 2. Get Unpracticed Questions by Chapter (with Question Set)

**Endpoint**: `GET /questions/unpracticed/chapter`

**Parameters**:
- `studentId` (required): Student ID
- `chapterId` (required): Chapter ID
- `limit` (optional, default=10): Number of questions to return
- `level` (optional): Filter by difficulty level (Easy, Medium, Hard, or "all")
- `usageType` (optional): Filter by usage type (Practice, Test, Both, or "all")
- `sectionId` (optional): Filter by section ID
- `questionSet` (optional): Filter by question set (e.g., "1", "2", "3", or "all")

**Example Requests**:

```http
# Get Set 1 questions from a chapter
GET /questions/unpracticed/chapter?studentId=1&chapterId=LC5H0101&limit=10&questionSet=1

# Get Set 2 questions with difficulty filter
GET /questions/unpracticed/chapter?studentId=1&chapterId=LC5H0101&limit=10&level=Medium&questionSet=2

# Get Set 3 questions from specific section
GET /questions/unpracticed/chapter?studentId=1&chapterId=LC5H0101&limit=10&sectionId=S1&questionSet=3
```

---

### 3. Get Unpracticed Questions by Chapter with Flags

**Endpoint**: `GET /questions/unpracticed/chapter/with-flags`

**Parameters**: Same as endpoint #2

**Example Request**:
```http
GET /questions/unpracticed/chapter/with-flags?studentId=1&chapterId=LC5H0101&limit=10&questionSet=2
```

---

### 4. Get Unpracticed Questions by Subject with Flags

**Endpoint**: `GET /questions/unpracticed/subject/with-flags`

**Parameters**: Same as endpoint #1

**Example Request**:
```http
GET /questions/unpracticed/subject/with-flags?studentId=1&subjectId=MATH&limit=10&questionSet=2
```

---

### 5. Create Questions (with Question Set)

**Endpoint**: `POST /questions`

**Request Body**:
```json
[
  {
    "questionText": "What is 5 + 5?",
    "optionA": "8",
    "optionB": "9",
    "optionC": "10",
    "optionD": "11",
    "correctAnswer": "C",
    "questionType": 1,
    "difficultyLevel": "Easy",
    "usageType": "Practice",
    "questionSet": "2",
    "chapter": {
      "chapterId": "LC5H0101"
    }
  }
]
```

---

### 6. Update Question (with Question Set)

**Endpoint**: `PUT /questions/{id}`

**Request Body**:
```json
{
  "questionText": "Updated question text",
  "optionA": "Option A",
  "optionB": "Option B",
  "optionC": "Option C",
  "optionD": "Option D",
  "correctAnswer": "B",
  "questionSet": "3"
}
```

---

### 7. Upload Questions from Excel (with Question Set)

**Endpoint**: `POST /questions/upload`

**Excel Format**:

| Column | Field Name | Required | Default | Example |
|--------|------------|----------|---------|---------|
| A | chapterId | Yes | - | LC5H0101 |
| B | questionText | No* | - | What is 2+2? |
| C | correctAnswer | No | - | B |
| D | optionA | No | - | 3 |
| E | optionB | No | - | 4 |
| F | optionC | No | - | 5 |
| G | optionD | No | - | 6 |
| H | optionE | No | - | 7 |
| I | optionF | No | - | 8 |
| J | explanation | No | - | Addition |
| K | difficultyLevel | No | - | Easy |
| L | questionType | No | 1 | 1 |
| M | correctAnswers | No | - | B,C |
| N | notes | No | - | Basic math |
| O | usageType | No | Both | Practice |
| **P** | **questionSet** | **No** | **1** | **2** |

*questionText can be null if questionImageUrl is provided

**Example Excel Row**:
```
LC5H0101 | What is 10+10? | C | 15 | 18 | 20 | 22 | | | Addition | Easy | 1 | | | Practice | 2
```

---

## Usage Scenarios

### Scenario 1: Progressive Practice

1. **Student starts with Set 1**:
   ```http
   GET /questions/unpracticed/chapter?studentId=1&chapterId=LC5H0101&limit=10&questionSet=1
   ```

2. **Student completes all Set 1 questions**

3. **Student moves to Set 2 for more practice**:
   ```http
   GET /questions/unpracticed/chapter?studentId=1&chapterId=LC5H0101&limit=10&questionSet=2
   ```

4. **Student continues to Set 3, 4, etc.**

### Scenario 2: Mixed Practice (All Sets)

Get questions from all sets (default behavior):
```http
GET /questions/unpracticed/chapter?studentId=1&chapterId=LC5H0101&limit=10
```

### Scenario 3: Specific Set for Testing

Get only Set 2 questions for a specific test:
```http
GET /questions/unpracticed/chapter?studentId=1&chapterId=LC5H0101&limit=20&usageType=Test&questionSet=2
```

---

## Filtering Logic

### Question Set Parameter Behavior

| Parameter Value | Behavior |
|----------------|----------|
| `null` (not provided) | Returns questions from **all sets** (backward compatible) |
| `""` (empty string) | Returns questions from **all sets** |
| `"all"` | Returns questions from **all sets** |
| `"1"` | Returns only **Set 1** questions |
| `"2"` | Returns only **Set 2** questions |
| `"3"` | Returns only **Set 3** questions |
| Any other value | Returns questions matching that specific set |

### Case Sensitivity

- Question set filtering is **case-insensitive**
- `"1"` and `"1"` are treated the same
- `"Set1"` and `"set1"` are treated the same

---

## Data Model

### Question Object (with questionSet)

```json
{
  "questionId": 123,
  "sectionId": "LC5H0101-S1",
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
  "difficultyLevel": "Easy",
  "explanation": "Basic addition",
  "notes": null,
  "hint": "Add the numbers",
  "usageType": "Practice",
  "questionImageUrl": null,
  "questionSet": "1",
  "isCustom": false,
  "createdBy": null,
  "visibility": "PUBLIC"
}
```

---

## Backward Compatibility

### Existing Questions

- All existing questions are automatically assigned to **Set 1** during migration
- No changes required to existing API calls
- If `questionSet` parameter is not provided, all sets are returned (current behavior)

### Existing API Calls

All existing API calls continue to work without modification:

```http
# This still works and returns questions from all sets
GET /questions/unpracticed/chapter?studentId=1&chapterId=LC5H0101&limit=10
```

---

## Best Practices

### 1. Organizing Questions into Sets

- **Set 1**: Initial practice questions (easier, foundational)
- **Set 2**: Additional practice (similar difficulty, different problems)
- **Set 3**: Advanced practice (slightly harder variations)
- **Set 4+**: Expert level or exam preparation

### 2. Naming Convention

Use simple numeric values for sets:
- ✅ Good: `"1"`, `"2"`, `"3"`
- ⚠️ Acceptable: `"Set1"`, `"SetA"`, `"Advanced"`
- ❌ Avoid: Long strings, special characters

### 3. Progressive Learning Flow

```
Student Flow:
1. Complete Set 1 → Check progress
2. If mastered → Move to Set 2
3. If struggling → Review Set 1 again
4. Continue to Set 3, 4, etc.
```

### 4. Excel Upload

When uploading questions via Excel:
- Leave column P empty for Set 1 (default)
- Specify "2", "3", etc. for other sets
- Keep sets consistent within the same chapter/topic

---

## Testing

### Test Cases

1. **Test Set 1 Questions**:
   ```http
   GET /questions/unpracticed/chapter?studentId=1&chapterId=LC5H0101&questionSet=1
   ```
   Expected: Only Set 1 questions returned

2. **Test Set 2 Questions**:
   ```http
   GET /questions/unpracticed/chapter?studentId=1&chapterId=LC5H0101&questionSet=2
   ```
   Expected: Only Set 2 questions returned

3. **Test All Sets (no parameter)**:
   ```http
   GET /questions/unpracticed/chapter?studentId=1&chapterId=LC5H0101
   ```
   Expected: Questions from all sets returned

4. **Test All Sets (explicit)**:
   ```http
   GET /questions/unpracticed/chapter?studentId=1&chapterId=LC5H0101&questionSet=all
   ```
   Expected: Questions from all sets returned

5. **Test Create with Set**:
   ```http
   POST /questions
   Body: { ..., "questionSet": "2" }
   ```
   Expected: Question created with Set 2

6. **Test Update with Set**:
   ```http
   PUT /questions/123
   Body: { ..., "questionSet": "3" }
   ```
   Expected: Question updated to Set 3

---

## Migration Checklist

- [x] Run database migration script
- [x] Update Question entity
- [x] Update QuestionRepository queries
- [x] Update QuestionService methods
- [x] Update QuestionController endpoints
- [x] Update Excel upload functionality
- [x] Test all endpoints
- [ ] Update frontend to support questionSet parameter
- [ ] Update API documentation
- [ ] Train content creators on set organization

---

## Support

For questions or issues related to the Question Set feature, please contact the development team.

---

**Last Updated**: 2026-05-23  
**Version**: 1.0  
**Author**: Bob