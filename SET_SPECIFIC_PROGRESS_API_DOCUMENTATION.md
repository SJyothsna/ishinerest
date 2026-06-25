# Set-Specific Progress Tracking API Documentation

## Overview

The Practice Progress and Test Progress endpoints now support set-specific tracking for Set 1 and Set 2 questions. The API automatically manages which sets to display based on Set 1 completion status.

## Business Logic

### Display Rules

1. **Set 1 Incomplete**: Show only Set 1 progress in main fields
   - Main fields reflect Set 1 only
   - Set 1 breakdown populated
   - Set 2 breakdown populated (but not included in main totals)
   - `set1Complete` = false

2. **Set 1 Complete**: Automatically show combined Set 1 + Set 2 progress
   - Main fields reflect Set 1 + Set 2 combined totals
   - Both Set 1 and Set 2 breakdowns populated
   - `set1Complete` = true
   - Applies even if Set 2 not started yet

## API Endpoints

### 1. Get Practice Progress by Chapter

**Endpoint**: `GET /students/{studentId}/practiceProgress/chapter`

**Parameters**:
- `studentId` (path, required): Student ID
- `chapterId` (query, required): Chapter ID

**Example Request**:
```http
GET /students/1/practiceProgress/chapter?chapterId=LC5H0101
```

**Response Structure**:
```json
{
  "totalQuestions": 50,
  "practicedQuestions": 20,
  "correctAnswers": 15,
  "incorrectAnswers": 5,
  "notPracticed": 30,
  "set1Total": 50,
  "set1Practiced": 20,
  "set1Correct": 15,
  "set1Incorrect": 5,
  "set1NotPracticed": 30,
  "set2Total": 45,
  "set2Practiced": 0,
  "set2Correct": 0,
  "set2Incorrect": 0,
  "set2NotPracticed": 45,
  "set1Complete": false
}
```

---

### 2. Get Practice Progress by Subject

**Endpoint**: `GET /students/{studentId}/practiceProgress/subject`

**Parameters**:
- `studentId` (path, required): Student ID
- `subjectId` (query, required): Subject ID

**Example Request**:
```http
GET /students/1/practiceProgress/subject?subjectId=MATH
```

**Response**: Same structure as chapter progress

---

### 3. Get Test Progress by Chapter

**Endpoint**: `GET /students/{studentId}/testProgress/chapter`

**Parameters**:
- `studentId` (path, required): Student ID
- `chapterId` (query, required): Chapter ID

**Example Request**:
```http
GET /students/1/testProgress/chapter?chapterId=LC5H0101
```

**Response**: Same structure as practice progress

---

### 4. Get Test Progress by Subject

**Endpoint**: `GET /students/{studentId}/testProgress/subject`

**Parameters**:
- `studentId` (path, required): Student ID
- `subjectId` (query, required): Subject ID

**Example Request**:
```http
GET /students/1/testProgress/subject?subjectId=MATH
```

**Response**: Same structure as practice progress

---

## Response Field Descriptions

### Main Fields
These fields show either Set 1 only OR Set 1 + Set 2 combined, depending on `set1Complete`:

| Field | Type | Description |
|-------|------|-------------|
| `totalQuestions` | Long | Total questions available (Set 1 only OR Set 1 + Set 2) |
| `practicedQuestions` | Long | Questions attempted (Set 1 only OR Set 1 + Set 2) |
| `correctAnswers` | Long | Correct answers (Set 1 only OR Set 1 + Set 2) |
| `incorrectAnswers` | Long | Incorrect answers (Set 1 only OR Set 1 + Set 2) |
| `notPracticed` | Long | Questions not yet attempted (Set 1 only OR Set 1 + Set 2) |

### Set 1 Breakdown
Always populated with Set 1 specific data:

| Field | Type | Description |
|-------|------|-------------|
| `set1Total` | Long | Total Set 1 questions |
| `set1Practiced` | Long | Set 1 questions attempted |
| `set1Correct` | Long | Set 1 correct answers |
| `set1Incorrect` | Long | Set 1 incorrect answers |
| `set1NotPracticed` | Long | Set 1 questions not attempted |

### Set 2 Breakdown
Always populated with Set 2 specific data:

| Field | Type | Description |
|-------|------|-------------|
| `set2Total` | Long | Total Set 2 questions |
| `set2Practiced` | Long | Set 2 questions attempted |
| `set2Correct` | Long | Set 2 correct answers |
| `set2Incorrect` | Long | Set 2 incorrect answers |
| `set2NotPracticed` | Long | Set 2 questions not attempted |

### Completion Flag

| Field | Type | Description |
|-------|------|-------------|
| `set1Complete` | Boolean | `true` if all Set 1 questions practiced, `false` otherwise |

---

## Response Examples

### Scenario 1: Set 1 Incomplete (40% complete)

Student has practiced 20 out of 50 Set 1 questions. Set 2 has 45 questions but none practiced yet.

**Request**:
```http
GET /students/1/practiceProgress/chapter?chapterId=LC5H0101
```

**Response**:
```json
{
  "totalQuestions": 50,
  "practicedQuestions": 20,
  "correctAnswers": 15,
  "incorrectAnswers": 5,
  "notPracticed": 30,
  "set1Total": 50,
  "set1Practiced": 20,
  "set1Correct": 15,
  "set1Incorrect": 5,
  "set1NotPracticed": 30,
  "set2Total": 45,
  "set2Practiced": 0,
  "set2Correct": 0,
  "set2Incorrect": 0,
  "set2NotPracticed": 45,
  "set1Complete": false
}
```

**Interpretation**:
- Main fields show Set 1 only (50 total, 20 practiced)
- Set 1 is 40% complete (20/50)
- Set 2 data available but not included in main totals
- `set1Complete` = false

---

### Scenario 2: Set 1 Complete, Set 2 Not Started

Student has completed all 50 Set 1 questions. Set 2 has 45 questions but none practiced yet.

**Request**:
```http
GET /students/1/practiceProgress/chapter?chapterId=LC5H0101
```

**Response**:
```json
{
  "totalQuestions": 95,
  "practicedQuestions": 50,
  "correctAnswers": 42,
  "incorrectAnswers": 8,
  "notPracticed": 45,
  "set1Total": 50,
  "set1Practiced": 50,
  "set1Correct": 42,
  "set1Incorrect": 8,
  "set1NotPracticed": 0,
  "set2Total": 45,
  "set2Practiced": 0,
  "set2Correct": 0,
  "set2Incorrect": 0,
  "set2NotPracticed": 45,
  "set1Complete": true
}
```

**Interpretation**:
- Main fields now show combined Set 1 + Set 2 (95 total, 50 practiced)
- Set 1 is 100% complete (50/50)
- Set 2 is 0% complete (0/45)
- Overall progress is 52.6% (50/95)
- `set1Complete` = true

---

### Scenario 3: Set 1 Complete, Set 2 In Progress

Student has completed all 50 Set 1 questions and practiced 20 out of 45 Set 2 questions.

**Request**:
```http
GET /students/1/practiceProgress/chapter?chapterId=LC5H0101
```

**Response**:
```json
{
  "totalQuestions": 95,
  "practicedQuestions": 70,
  "correctAnswers": 58,
  "incorrectAnswers": 12,
  "notPracticed": 25,
  "set1Total": 50,
  "set1Practiced": 50,
  "set1Correct": 42,
  "set1Incorrect": 8,
  "set1NotPracticed": 0,
  "set2Total": 45,
  "set2Practiced": 20,
  "set2Correct": 16,
  "set2Incorrect": 4,
  "set2NotPracticed": 25,
  "set1Complete": true
}
```

**Interpretation**:
- Main fields show combined Set 1 + Set 2 (95 total, 70 practiced)
- Set 1 is 100% complete (50/50)
- Set 2 is 44.4% complete (20/45)
- Overall progress is 73.7% (70/95)
- `set1Complete` = true

---

### Scenario 4: Both Sets Complete

Student has completed all questions in both sets.

**Request**:
```http
GET /students/1/practiceProgress/chapter?chapterId=LC5H0101
```

**Response**:
```json
{
  "totalQuestions": 95,
  "practicedQuestions": 95,
  "correctAnswers": 80,
  "incorrectAnswers": 15,
  "notPracticed": 0,
  "set1Total": 50,
  "set1Practiced": 50,
  "set1Correct": 42,
  "set1Incorrect": 8,
  "set1NotPracticed": 0,
  "set2Total": 45,
  "set2Practiced": 45,
  "set2Correct": 38,
  "set2Incorrect": 7,
  "set2NotPracticed": 0,
  "set1Complete": true
}
```

**Interpretation**:
- Main fields show combined Set 1 + Set 2 (95 total, 95 practiced)
- Set 1 is 100% complete (50/50)
- Set 2 is 100% complete (45/45)
- Overall progress is 100% (95/95)
- `set1Complete` = true

---

### Scenario 5: No Questions Available

Chapter/subject has no questions in either set.

**Request**:
```http
GET /students/1/practiceProgress/chapter?chapterId=LC5H0199
```

**Response**:
```json
{
  "totalQuestions": 0,
  "practicedQuestions": 0,
  "correctAnswers": 0,
  "incorrectAnswers": 0,
  "notPracticed": 0,
  "set1Total": 0,
  "set1Practiced": 0,
  "set1Correct": 0,
  "set1Incorrect": 0,
  "set1NotPracticed": 0,
  "set2Total": 0,
  "set2Practiced": 0,
  "set2Correct": 0,
  "set2Incorrect": 0,
  "set2NotPracticed": 0,
  "set1Complete": false
}
```

---

## Frontend Integration Guide

### Displaying Progress

#### Option 1: Show Main Fields Only (Simplified)
```javascript
const progress = response.data;
const percentage = progress.totalQuestions > 0 
  ? (progress.practicedQuestions / progress.totalQuestions * 100).toFixed(1)
  : 0;

console.log(`Progress: ${percentage}% (${progress.practicedQuestions}/${progress.totalQuestions})`);
console.log(`Correct: ${progress.correctAnswers}, Incorrect: ${progress.incorrectAnswers}`);
```

#### Option 2: Show Set Breakdowns (Detailed)
```javascript
const progress = response.data;

// Set 1 Progress
const set1Percentage = progress.set1Total > 0
  ? (progress.set1Practiced / progress.set1Total * 100).toFixed(1)
  : 0;
console.log(`Set 1: ${set1Percentage}% (${progress.set1Practiced}/${progress.set1Total})`);

// Set 2 Progress (if Set 1 complete)
if (progress.set1Complete) {
  const set2Percentage = progress.set2Total > 0
    ? (progress.set2Practiced / progress.set2Total * 100).toFixed(1)
    : 0;
  console.log(`Set 2: ${set2Percentage}% (${progress.set2Practiced}/${progress.set2Total})`);
}

// Overall Progress
const overallPercentage = progress.totalQuestions > 0
  ? (progress.practicedQuestions / progress.totalQuestions * 100).toFixed(1)
  : 0;
console.log(`Overall: ${overallPercentage}%`);
```

#### Option 3: Conditional Display Based on Set 1 Completion
```javascript
const progress = response.data;

if (!progress.set1Complete) {
  // Show Set 1 only
  return {
    title: "Set 1 Progress",
    total: progress.set1Total,
    practiced: progress.set1Practiced,
    correct: progress.set1Correct,
    incorrect: progress.set1Incorrect,
    percentage: (progress.set1Practiced / progress.set1Total * 100).toFixed(1)
  };
} else {
  // Show combined progress with set breakdowns
  return {
    title: "Overall Progress",
    total: progress.totalQuestions,
    practiced: progress.practicedQuestions,
    correct: progress.correctAnswers,
    incorrect: progress.incorrectAnswers,
    percentage: (progress.practicedQuestions / progress.totalQuestions * 100).toFixed(1),
    sets: [
      {
        name: "Set 1",
        total: progress.set1Total,
        practiced: progress.set1Practiced,
        percentage: 100
      },
      {
        name: "Set 2",
        total: progress.set2Total,
        practiced: progress.set2Practiced,
        percentage: (progress.set2Practiced / progress.set2Total * 100).toFixed(1)
      }
    ]
  };
}
```

---

## Calculation Examples

### Progress Percentage
```
percentage = (practicedQuestions / totalQuestions) * 100
```

### Accuracy Rate
```
accuracy = (correctAnswers / practicedQuestions) * 100
```

### Set 1 Completion Check
```
set1Complete = (set1Practiced >= set1Total) && (set1Total > 0)
```

### Combined Totals (when Set 1 complete)
```
totalQuestions = set1Total + set2Total
practicedQuestions = set1Practiced + set2Practiced
correctAnswers = set1Correct + set2Correct
incorrectAnswers = set1Incorrect + set2Incorrect
notPracticed = totalQuestions - practicedQuestions
```

---

## Error Responses

### Student Not Found
```json
{
  "timestamp": "2026-05-23T22:00:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Student not found with ID: 999",
  "path": "/students/999/practiceProgress/chapter"
}
```

### Invalid Chapter/Subject ID
Returns zero values for all fields (no error thrown).

---

## Backward Compatibility

### Legacy Clients
Clients that only use the main fields (`totalQuestions`, `practicedQuestions`, etc.) will continue to work without modification. The behavior changes slightly:

**Before**: Main fields always showed all questions regardless of set
**After**: Main fields show Set 1 only until complete, then Set 1 + Set 2

### Migration Strategy
1. Update frontend to use new set-specific fields
2. Add UI to show set breakdowns
3. Display `set1Complete` status to users
4. Gradually phase out reliance on main fields alone

---

## Testing Checklist

- [ ] Test with Set 1 incomplete (0%, 50%, 99%)
- [ ] Test with Set 1 exactly 100% complete
- [ ] Test with Set 1 complete and Set 2 not started
- [ ] Test with Set 1 complete and Set 2 in progress
- [ ] Test with both sets complete
- [ ] Test with no questions available
- [ ] Test with only Set 1 questions (no Set 2)
- [ ] Test with only Set 2 questions (no Set 1)
- [ ] Verify all four endpoints (practice/test × chapter/subject)
- [ ] Verify accuracy calculations
- [ ] Verify percentage calculations

---

## Support

For questions or issues related to the Set-Specific Progress Tracking feature, please contact the development team.

---

**Last Updated**: 2026-05-23  
**Version**: 1.0  
**Author**: Bob