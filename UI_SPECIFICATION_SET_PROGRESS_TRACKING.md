# UI Specification: Set-Specific Progress Tracking

## For Frontend Team

**Date**: 2026-05-23  
**Backend Version**: 1.0  
**Breaking Changes**: ❌ None (Fully Backward Compatible)

---

## Summary

The Practice Progress and Test Progress API endpoints now return additional fields for Set 1 and Set 2 question tracking. The existing fields remain unchanged, so your current UI will continue to work without modification.

---

## What Changed?

### API Endpoints (URLs Unchanged)
- `GET /students/{studentId}/practiceProgress/chapter?chapterId={id}`
- `GET /students/{studentId}/practiceProgress/subject?subjectId={id}`
- `GET /students/{studentId}/testProgress/chapter?chapterId={id}`
- `GET /students/{studentId}/testProgress/subject?subjectId={id}`

### Response Structure

#### Before (5 fields)
```json
{
  "totalQuestions": 50,
  "practicedQuestions": 20,
  "correctAnswers": 15,
  "incorrectAnswers": 5,
  "notPracticed": 30
}
```

#### After (16 fields - 11 new fields added)
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

## New Fields Explained

### Main Fields (Existing - Behavior Changed)
These fields now show **Set 1 only** until Set 1 is complete, then **Set 1 + Set 2 combined**:

| Field | Type | Description |
|-------|------|-------------|
| `totalQuestions` | number | Total questions (Set 1 OR Set 1+Set 2) |
| `practicedQuestions` | number | Questions practiced (Set 1 OR Set 1+Set 2) |
| `correctAnswers` | number | Correct answers (Set 1 OR Set 1+Set 2) |
| `incorrectAnswers` | number | Incorrect answers (Set 1 OR Set 1+Set 2) |
| `notPracticed` | number | Not practiced (Set 1 OR Set 1+Set 2) |

### Set 1 Breakdown (New)
Always populated with Set 1 specific data:

| Field | Type | Description |
|-------|------|-------------|
| `set1Total` | number | Total Set 1 questions |
| `set1Practiced` | number | Set 1 questions practiced |
| `set1Correct` | number | Set 1 correct answers |
| `set1Incorrect` | number | Set 1 incorrect answers |
| `set1NotPracticed` | number | Set 1 not practiced |

### Set 2 Breakdown (New)
Always populated with Set 2 specific data:

| Field | Type | Description |
|-------|------|-------------|
| `set2Total` | number | Total Set 2 questions |
| `set2Practiced` | number | Set 2 questions practiced |
| `set2Correct` | number | Set 2 correct answers |
| `set2Incorrect` | number | Set 2 incorrect answers |
| `set2NotPracticed` | number | Set 2 not practiced |

### Completion Flag (New)
| Field | Type | Description |
|-------|------|-------------|
| `set1Complete` | boolean | `true` if all Set 1 questions practiced |

---

## Business Logic for UI

### Rule 1: When to Show Set 2
```javascript
if (response.set1Complete === true) {
  // Show Set 2 information
  // Main fields now include Set 1 + Set 2 combined
} else {
  // Hide or gray out Set 2 information
  // Main fields show Set 1 only
}
```

### Rule 2: Progress Calculation
```javascript
// Overall progress percentage
const overallProgress = response.totalQuestions > 0 
  ? (response.practicedQuestions / response.totalQuestions * 100).toFixed(1)
  : 0;

// Set 1 progress percentage
const set1Progress = response.set1Total > 0
  ? (response.set1Practiced / response.set1Total * 100).toFixed(1)
  : 0;

// Set 2 progress percentage (only if Set 1 complete)
const set2Progress = response.set1Complete && response.set2Total > 0
  ? (response.set2Practiced / response.set2Total * 100).toFixed(1)
  : 0;
```

### Rule 3: Accuracy Calculation
```javascript
// Overall accuracy
const accuracy = response.practicedQuestions > 0
  ? (response.correctAnswers / response.practicedQuestions * 100).toFixed(1)
  : 0;

// Set 1 accuracy
const set1Accuracy = response.set1Practiced > 0
  ? (response.set1Correct / response.set1Practiced * 100).toFixed(1)
  : 0;

// Set 2 accuracy
const set2Accuracy = response.set2Practiced > 0
  ? (response.set2Correct / response.set2Practiced * 100).toFixed(1)
  : 0;
```

---

## UI Implementation Options

### Option 1: Minimal Changes (Keep Current UI)
**No changes required!** Your existing UI will continue to work because:
- All original fields are still present
- You can ignore the new fields
- Main fields show appropriate totals

### Option 2: Show Set Indicator (Recommended)
Add a simple indicator showing which set(s) are included:

```jsx
<div className="progress-header">
  <h3>Progress</h3>
  {!response.set1Complete && <span className="badge">Set 1</span>}
  {response.set1Complete && <span className="badge">Set 1 + Set 2</span>}
</div>
```

### Option 3: Show Set Breakdowns (Enhanced)
Display detailed breakdown for each set:

```jsx
<div className="progress-container">
  {/* Overall Progress */}
  <div className="overall-progress">
    <h3>Overall Progress</h3>
    <ProgressBar 
      value={response.practicedQuestions} 
      max={response.totalQuestions} 
    />
    <p>{overallProgress}% Complete</p>
  </div>

  {/* Set 1 Breakdown */}
  <div className="set-breakdown">
    <h4>Set 1 {response.set1Complete && '✓'}</h4>
    <ProgressBar 
      value={response.set1Practiced} 
      max={response.set1Total} 
    />
    <p>{response.set1Practiced}/{response.set1Total} questions</p>
    <p>Accuracy: {set1Accuracy}%</p>
  </div>

  {/* Set 2 Breakdown (only if Set 1 complete) */}
  {response.set1Complete && (
    <div className="set-breakdown">
      <h4>Set 2</h4>
      <ProgressBar 
        value={response.set2Practiced} 
        max={response.set2Total} 
      />
      <p>{response.set2Practiced}/{response.set2Total} questions</p>
      <p>Accuracy: {set2Accuracy}%</p>
    </div>
  )}
</div>
```

---

## Example Scenarios

### Scenario 1: Set 1 Incomplete (40% done)
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

**UI Should Show**:
- Progress: 40% (20/50)
- "Set 1" badge or indicator
- Set 2 grayed out or hidden

---

### Scenario 2: Set 1 Complete, Set 2 Not Started
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

**UI Should Show**:
- Progress: 52.6% (50/95)
- "Set 1 + Set 2" badge or indicator
- Set 1: 100% complete ✓
- Set 2: 0% complete (now visible)

---

### Scenario 3: Set 1 Complete, Set 2 In Progress
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

**UI Should Show**:
- Progress: 73.7% (70/95)
- "Set 1 + Set 2" badge or indicator
- Set 1: 100% complete ✓
- Set 2: 44.4% complete (20/45)

---

## TypeScript Interface

```typescript
interface ProgressResponse {
  // Main fields (Set 1 only OR Set 1 + Set 2 combined)
  totalQuestions: number;
  practicedQuestions: number;
  correctAnswers: number;
  incorrectAnswers: number;
  notPracticed: number;
  
  // Set 1 breakdown
  set1Total: number;
  set1Practiced: number;
  set1Correct: number;
  set1Incorrect: number;
  set1NotPracticed: number;
  
  // Set 2 breakdown
  set2Total: number;
  set2Practiced: number;
  set2Correct: number;
  set2Incorrect: number;
  set2NotPracticed: number;
  
  // Completion flag
  set1Complete: boolean;
}
```

---

## Migration Checklist for UI Team

### Phase 1: Verify Compatibility (No Changes)
- [ ] Test existing UI with new API response
- [ ] Verify all existing features work
- [ ] Confirm no errors in console

### Phase 2: Add Set Indicator (Optional)
- [ ] Add badge/label showing "Set 1" or "Set 1 + Set 2"
- [ ] Update based on `set1Complete` flag
- [ ] Test with different scenarios

### Phase 3: Add Set Breakdowns (Optional)
- [ ] Create UI components for set breakdowns
- [ ] Show Set 1 progress always
- [ ] Show Set 2 progress only when `set1Complete === true`
- [ ] Add progress bars for each set
- [ ] Display accuracy for each set

### Phase 4: Polish (Optional)
- [ ] Add animations for set transitions
- [ ] Add tooltips explaining sets
- [ ] Add celebration when Set 1 completes
- [ ] Add "Unlock Set 2" messaging

---

## Testing Checklist

- [ ] Test with Set 1 incomplete (0%, 50%, 99%)
- [ ] Test with Set 1 exactly 100% complete
- [ ] Test with Set 1 complete, Set 2 not started
- [ ] Test with Set 1 complete, Set 2 in progress
- [ ] Test with both sets complete
- [ ] Test with no questions available
- [ ] Test all four endpoints (practice/test × chapter/subject)
- [ ] Verify calculations are correct
- [ ] Verify UI updates when set1Complete changes

---

## FAQs

### Q: Do I need to update my UI immediately?
**A**: No! Your existing UI will continue to work. The new fields are additions, not replacements.

### Q: What if I ignore the new fields?
**A**: Your UI will work fine, but users won't see the set breakdowns. The main fields will show appropriate totals.

### Q: When should I show Set 2 information?
**A**: Only when `set1Complete === true`. Before that, Set 2 should be hidden or grayed out.

### Q: What does "Set 1 Complete" mean?
**A**: It means the student has practiced all Set 1 questions at least once (regardless of correctness).

### Q: Can Set 2 be practiced before Set 1 is complete?
**A**: No, the backend logic prevents this. Set 2 only appears after Set 1 is 100% complete.

### Q: What if there are no Set 2 questions?
**A**: `set2Total` will be 0. You can hide Set 2 UI if `set2Total === 0`.

---

## Support

For questions or issues:
- Backend API: Contact backend team
- UI Implementation: Refer to this specification
- Testing: Use the HTTP test file provided

---

**Document Version**: 1.0  
**Last Updated**: 2026-05-23  
**Author**: Bob (Backend Team)