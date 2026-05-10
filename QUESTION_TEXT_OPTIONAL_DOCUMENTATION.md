# Question Text Optional Feature

## Overview
This feature makes the `questionText` field optional when a question has an image (`questionImageUrl`). This allows administrators to create questions that rely solely on images without requiring text.

## Changes Made

### 1. Frontend Changes (Admin Panel)
**File:** `src/main/resources/templates/admin.html`

**Changes:**
- Removed `required` attribute from the `questionText` textarea field
- Updated label to show conditional requirement: "Question Text *" with helper text "Required unless a question image is provided"
- Added custom JavaScript validation in form submit handler to ensure either `questionText` or an image file is provided
- Modified `questionData` object to send `null` for empty question text instead of empty string

**Validation Logic:**
```javascript
// Validate that either questionText or image file is provided
const questionText = document.getElementById('questionText').value.trim();
const imageFile = document.getElementById('questionImage').files[0];
const existingImageUrl = document.getElementById('questionImageUrl').value;

if (!questionText && !imageFile && !existingImageUrl) {
    showAlert('question-alert', 'Please provide either question text or upload a question image', 'error');
    return;
}
```

### 2. Database Schema Change
**File:** `database_migration_make_question_text_nullable.sql`

The `question_text` column in the `questions` table has been changed from `NOT NULL` to `NULL`:

```sql
ALTER TABLE questions MODIFY COLUMN question_text TEXT NULL;
```

**Migration Steps:**
1. Connect to your MySQL database
2. Run the migration script:
   ```bash
   mysql -u your_username -p your_database < database_migration_make_question_text_nullable.sql
   ```
3. Verify the change:
   ```sql
   DESCRIBE questions;
   ```
   The `question_text` column should show `NULL: YES`

### 2. Entity Update
**File:** `src/main/java/com/ishine/ishinerest/entity/Question.java`

Removed the `nullable = false` constraint from the `questionText` field:

```java
// Before:
@Column(nullable = false, columnDefinition = "TEXT")
private String questionText;

// After:
@Column(columnDefinition = "TEXT")
private String questionText;
```

### 3. Validation Logic
**File:** `src/main/java/com/ishine/ishinerest/service/QuestionService.java`

Added custom validation to ensure at least one of `questionText` or `questionImageUrl` is provided:

```java
private void validateQuestion(Question question) {
    String questionText = question.getQuestionText();
    String questionImageUrl = question.getQuestionImageUrl();
    
    boolean hasText = questionText != null && !questionText.trim().isEmpty();
    boolean hasImage = questionImageUrl != null && !questionImageUrl.trim().isEmpty();
    
    if (!hasText && !hasImage) {
        throw new IllegalArgumentException("Question must have either question text or question image");
    }
}
```

This validation is called in:
- `saveQuestion(Question question)` - for single question creation/update
- `saveQuestions(List<Question> questions)` - for bulk question creation

## Usage Examples

### Valid Scenarios

#### 1. Question with Image Only
```json
{
  "questionText": null,
  "optionA": "Option A",
  "optionB": "Option B",
  "optionC": "Option C",
  "optionD": "Option D",
  "correctAnswer": "A",
  "questionType": 1,
  "difficultyLevel": "Easy",
  "questionImageUrl": "http://localhost:8080/uploads/chapters/LC5H0102/questions/q_123.png",
  "chapter": {
    "chapterId": "LC5H0102"
  }
}
```

#### 2. Question with Text Only
```json
{
  "questionText": "What is 2 + 2?",
  "optionA": "3",
  "optionB": "4",
  "optionC": "5",
  "optionD": "6",
  "correctAnswer": "B",
  "questionType": 1,
  "difficultyLevel": "Easy",
  "questionImageUrl": null,
  "chapter": {
    "chapterId": "LC5H0102"
  }
}
```

#### 3. Question with Both Text and Image
```json
{
  "questionText": "What is shown in the diagram?",
  "optionA": "Triangle",
  "optionB": "Square",
  "optionC": "Circle",
  "optionD": "Rectangle",
  "correctAnswer": "C",
  "questionType": 1,
  "difficultyLevel": "Easy",
  "questionImageUrl": "http://localhost:8080/uploads/chapters/LC5H0102/questions/q_124.png",
  "chapter": {
    "chapterId": "LC5H0102"
  }
}
```

### Invalid Scenario

#### Question with Neither Text nor Image (Will Fail)
```json
{
  "questionText": null,
  "optionA": "Option A",
  "optionB": "Option B",
  "optionC": "Option C",
  "optionD": "Option D",
  "correctAnswer": "A",
  "questionType": 1,
  "difficultyLevel": "Easy",
  "questionImageUrl": null,
  "chapter": {
    "chapterId": "LC5H0102"
  }
}
```

**Error Response:**
```json
{
  "error": "Question must have either question text or question image"
}
```

## API Endpoints Affected

All question creation and update endpoints now support optional question text:

- `POST /questions` - Create questions
- `PUT /questions/{id}` - Update question
- `POST /questions/upload` - Upload questions from Excel

## Testing

Test cases have been added to `src/test/java/rest/questions/questions.http`:

1. **Create question with image only** - Should succeed
2. **Create question with both text and image** - Should succeed
3. **Create question with neither text nor image** - Should fail with validation error

To test:
1. Ensure the database migration has been applied
2. Start the application
3. Use the HTTP test file to send requests
4. Verify responses match expected behavior

## Admin Panel Impact

In the admin panel's "Add Question" form:
- The question text field is now optional when an image is uploaded
- At least one of the following must be provided:
  - Question text
  - Question image
- The form should validate this on the client side before submission
- Server-side validation will catch any invalid submissions

## Backward Compatibility

✅ **Fully backward compatible**
- Existing questions with text continue to work
- Questions without images continue to work
- No changes required to existing data

## Notes

- Empty strings are treated the same as null values
- Whitespace-only strings are considered empty
- The validation applies to both single and bulk question operations
- Excel upload functionality respects the same validation rules