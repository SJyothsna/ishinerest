# Frontend Progress Bar Implementation Guide

## Overview

The backend has been updated to support Set-based progress tracking. This document provides step-by-step instructions for updating the frontend to display this information.

**Status**: ✅ Backend Complete | 🔄 Frontend Implementation Needed

---

## What Changed in Backend

### API Endpoints (URLs Unchanged)

The following endpoints now return enhanced data with Set 1 and Set 2 breakdowns:

1. **Practice Progress:**
   - `GET /students/{studentId}/practiceProgress/chapter?chapterId={id}`
   - `GET /students/{studentId}/practiceProgress/subject?subjectId={id}`

2. **Test Progress:**
   - `GET /students/{studentId}/testProgress/chapter?chapterId={id}`
   - `GET /students/{studentId}/testProgress/subject?subjectId={id}`

### Response Structure Change

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

#### After (16 fields - 11 NEW)
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

### TypeScript Interface

```typescript
interface ProgressResponse {
  // Main fields (Set 1 only OR Set 1 + Set 2 combined based on set1Complete)
  totalQuestions: number;
  practicedQuestions: number;
  correctAnswers: number;
  incorrectAnswers: number;
  notPracticed: number;
  
  // Set 1 breakdown (NEW)
  set1Total: number;
  set1Practiced: number;
  set1Correct: number;
  set1Incorrect: number;
  set1NotPracticed: number;
  
  // Set 2 breakdown (NEW)
  set2Total: number;
  set2Practiced: number;
  set2Correct: number;
  set2Incorrect: number;
  set2NotPracticed: number;
  
  // Completion flag (NEW)
  set1Complete: boolean;
}
```

---

## Business Logic

### Key Rule: Main Fields Behavior

**Before Set 1 Complete (`set1Complete === false`):**
- `totalQuestions` = Set 1 total only
- `practicedQuestions` = Set 1 practiced only
- Progress bar shows Set 1 progress

**After Set 1 Complete (`set1Complete === true`):**
- `totalQuestions` = Set 1 + Set 2 combined
- `practicedQuestions` = Set 1 + Set 2 combined
- Progress bar shows combined progress

### Important Note

When Set 1 completes, the progress percentage may **drop**:
- Before: 50/50 = 100%
- After: 50/95 = 52.6% (Set 2 added automatically)

This is expected behavior!

---

## Frontend Implementation

### Phase 1: Minimal Changes (Recommended First)

**Goal:** Add visual indicator showing which set(s) are displayed

**Changes Required:**

1. **Add Set Completion Badge**
```jsx
<Card.Title className="mb-0">
  📘 Your Progress - {contextLabel}
  {progress?.set1Complete && (
    <span className="badge bg-success ms-2">Set 1 Complete ✓</span>
  )}
</Card.Title>
```

2. **Add Phase Indicator**
```jsx
<div className="mt-2 text-muted small">
  {!progress?.set1Complete && (
    <span>📚 Currently showing Set 1 questions only</span>
  )}
  {progress?.set1Complete && (
    <span>📚 Showing Set 1 + Set 2 questions combined</span>
  )}
</div>
```

**Result:** Users understand why progress percentage changes when Set 1 completes.

---

### Phase 2: Enhanced Display (Optional)

**Goal:** Show detailed breakdown for each set

**Add Set Breakdown Section:**

```jsx
{/* Set Breakdown Accordion */}
{progress?.set1Total > 0 && (
  <Accordion className="mt-3">
    <Accordion.Item eventKey="0">
      <Accordion.Header>📊 Progress by Question Set</Accordion.Header>
      <Accordion.Body>
        
        {/* Set 1 Progress */}
        <div className="mb-3">
          <div className="d-flex justify-content-between align-items-center mb-2">
            <strong>Set 1 Questions</strong>
            {progress.set1Complete && (
              <span className="badge bg-success">Complete ✓</span>
            )}
          </div>
          
          <div className="d-flex justify-content-between mb-1">
            <div>
              <span className="text-success">✔ {progress.set1Correct}</span>
              <span className="ms-2 text-danger">✘ {progress.set1Incorrect}</span>
              <span className="ms-2 text-muted">⊘ {progress.set1NotPracticed}</span>
              <span className="ms-2">/ {progress.set1Total}</span>
            </div>
            <div>
              <strong>
                {progress.set1Total > 0 
                  ? Math.round((progress.set1Practiced / progress.set1Total) * 100) 
                  : 0}%
              </strong>
            </div>
          </div>
          
          <ProgressBar className="rounded">
            {progress.set1Correct > 0 && (
              <ProgressBar
                striped
                variant="success"
                now={(progress.set1Correct / progress.set1Total) * 100}
                key="set1-correct"
              />
            )}
            {progress.set1Incorrect > 0 && (
              <ProgressBar
                striped
                variant="danger"
                now={(progress.set1Incorrect / progress.set1Total) * 100}
                key="set1-incorrect"
              />
            )}
          </ProgressBar>
          
          {progress.set1Practiced > 0 && (
            <div className="mt-1 text-muted small">
              Accuracy: {Math.round((progress.set1Correct / progress.set1Practiced) * 100)}%
            </div>
          )}
        </div>

        {/* Set 2 Progress - Only show if Set 1 complete */}
        {progress.set1Complete && progress.set2Total > 0 && (
          <div className="mt-3 pt-3 border-top">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <strong>Set 2 Questions</strong>
              {progress.set2Practiced === progress.set2Total && progress.set2Total > 0 && (
                <span className="badge bg-success">Complete ✓</span>
              )}
            </div>
            
            <div className="d-flex justify-content-between mb-1">
              <div>
                <span className="text-success">✔ {progress.set2Correct}</span>
                <span className="ms-2 text-danger">✘ {progress.set2Incorrect}</span>
                <span className="ms-2 text-muted">⊘ {progress.set2NotPracticed}</span>
                <span className="ms-2">/ {progress.set2Total}</span>
              </div>
              <div>
                <strong>
                  {progress.set2Total > 0 
                    ? Math.round((progress.set2Practiced / progress.set2Total) * 100) 
                    : 0}%
                </strong>
              </div>
            </div>
            
            <ProgressBar className="rounded">
              {progress.set2Correct > 0 && (
                <ProgressBar
                  striped
                  variant="success"
                  now={(progress.set2Correct / progress.set2Total) * 100}
                  key="set2-correct"
                />
              )}
              {progress.set2Incorrect > 0 && (
                <ProgressBar
                  striped
                  variant="danger"
                  now={(progress.set2Incorrect / progress.set2Total) * 100}
                  key="set2-incorrect"
                />
              )}
            </ProgressBar>
            
            {progress.set2Practiced > 0 && (
              <div className="mt-1 text-muted small">
                Accuracy: {Math.round((progress.set2Correct / progress.set2Practiced) * 100)}%
              </div>
            )}
          </div>
        )}

        {/* Set 2 Locked Message */}
        {!progress.set1Complete && progress.set2Total > 0 && (
          <div className="mt-3 pt-3 border-top">
            <div className="alert alert-info mb-0">
              <strong>🔒 Set 2 Questions Locked</strong>
              <p className="mb-0 small">
                Complete all Set 1 questions to unlock Set 2!
              </p>
            </div>
          </div>
        )}
        
      </Accordion.Body>
    </Accordion.Item>
  </Accordion>
)}
```

---

## Testing Scenarios

### Scenario 1: Set 1 Incomplete (40%)

**API Response:**
```json
{
  "totalQuestions": 50,
  "practicedQuestions": 20,
  "set1Complete": false
}
```

**Expected UI:**
- Progress bar: 40% (20/50)
- Text: "Currently showing Set 1 questions only"
- No "Set 1 Complete" badge
- Set 2 locked or hidden

---

### Scenario 2: Set 1 Just Completed

**API Response:**
```json
{
  "totalQuestions": 95,
  "practicedQuestions": 50,
  "set1Complete": true
}
```

**Expected UI:**
- Progress bar: 52.6% (50/95) ⚠️ Drops from 100%!
- Text: "Showing Set 1 + Set 2 questions combined"
- "Set 1 Complete ✓" badge visible
- Set 2 section now visible

---

### Scenario 3: Both Sets In Progress

**API Response:**
```json
{
  "totalQuestions": 95,
  "practicedQuestions": 70,
  "set1Complete": true
}
```

**Expected UI:**
- Progress bar: 73.7% (70/95)
- Text: "Showing Set 1 + Set 2 questions combined"
- "Set 1 Complete ✓" badge visible
- Both sets visible in breakdown

---

## Quick Reference

### When to Show Set 2

```javascript
// Show Set 2 only when Set 1 is complete
if (progress.set1Complete === true) {
  // Display Set 2 information
} else {
  // Hide or show "locked" message
}
```

### Progress Calculations

```javascript
// Overall progress
const overallProgress = progress.totalQuestions > 0 
  ? Math.round((progress.practicedQuestions / progress.totalQuestions) * 100)
  : 0;

// Set 1 progress
const set1Progress = progress.set1Total > 0
  ? Math.round((progress.set1Practiced / progress.set1Total) * 100)
  : 0;

// Set 2 progress (only if Set 1 complete)
const set2Progress = progress.set1Complete && progress.set2Total > 0
  ? Math.round((progress.set2Practiced / progress.set2Total) * 100)
  : 0;
```

### Accuracy Calculations

```javascript
// Overall accuracy
const accuracy = progress.practicedQuestions > 0
  ? Math.round((progress.correctAnswers / progress.practicedQuestions) * 100)
  : 0;

// Set 1 accuracy
const set1Accuracy = progress.set1Practiced > 0
  ? Math.round((progress.set1Correct / progress.set1Practiced) * 100)
  : 0;

// Set 2 accuracy
const set2Accuracy = progress.set2Practiced > 0
  ? Math.round((progress.set2Correct / progress.set2Practiced) * 100)
  : 0;
```

---

## Important Notes

### ✅ Backward Compatible
- Existing UI will work without changes
- All original fields still present
- No breaking changes

### ⚠️ Progress May Drop
- When Set 1 completes, progress percentage drops
- This is expected: 50/50 (100%) → 50/95 (52.6%)
- Add indicator text to explain this to users

### 🔒 Set 2 Unlocking
- Set 2 only appears after Set 1 is 100% complete
- Backend automatically includes Set 2 in totals
- Frontend should show "locked" message before completion

---

## Testing Checklist

- [ ] Test with Set 1 incomplete (0%, 50%, 99%)
- [ ] Test with Set 1 exactly 100% complete
- [ ] Test with Set 1 complete, Set 2 not started
- [ ] Test with Set 1 complete, Set 2 in progress
- [ ] Test with both sets complete
- [ ] Verify progress percentage drops when Set 1 completes
- [ ] Verify "Set 1 Complete" badge appears correctly
- [ ] Verify Set 2 locked message shows before completion
- [ ] Test on mobile, tablet, and desktop
- [ ] Test with both practice and test progress

---

## Support

**Backend Documentation:**
- Full API Spec: `SET_SPECIFIC_PROGRESS_API_DOCUMENTATION.md`
- Implementation Details: `SET_SPECIFIC_PROGRESS_IMPLEMENTATION_SUMMARY.md`
- UI Specification: `UI_SPECIFICATION_SET_PROGRESS_TRACKING.md`

**Questions?**
Contact the backend team for clarification or assistance.

---

**Document Version**: 1.0  
**Last Updated**: 2026-05-23  
**Status**: Ready for Frontend Implementation