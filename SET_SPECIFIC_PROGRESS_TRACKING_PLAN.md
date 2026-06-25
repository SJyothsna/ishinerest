# Set-Specific Progress Tracking Implementation Plan

## Overview

This document outlines the plan to enhance the Practice Progress and Test Progress API endpoints to support set-specific tracking for Set 1 and Set 2 questions.

## Business Requirements

### Display Logic

1. **Set 1 Only Phase**: Show only Set 1 progress until Set 1 is 100% complete
   - Main fields (totalQuestions, practicedQuestions, etc.) show Set 1 data only
   - Set 1 breakdown fields populated
   - Set 2 breakdown fields are null/zero
   - `set1Complete` = false

2. **Set 1 + Set 2 Phase**: After Set 1 is 100% complete, automatically include Set 2
   - Main fields show combined Set 1 + Set 2 totals (even if Set 2 not started)
   - Both Set 1 and Set 2 breakdown fields populated
   - `set1Complete` = true

### Affected Endpoints

- `GET /students/{studentId}/practiceProgress/chapter?chapterId={id}`
- `GET /students/{studentId}/practiceProgress/subject?subjectId={id}`
- `GET /students/{studentId}/testProgress/chapter?chapterId={id}`
- `GET /students/{studentId}/testProgress/subject?subjectId={id}`

## Current Implementation Analysis

### Current Response Structure
```java
public class StudentPracticeProgressDTO {
    private Long totalQuestions;      // Total questions available
    private Long practicedQuestions;  // Questions attempted
    private Long correctAnswers;      // Correct answers
    private Long incorrectAnswers;    // Incorrect answers
    private Long notPracticed;        // Not yet attempted
}
```

### Current Data Flow
1. Controller receives request → calls StudentService
2. StudentService calls Repository (PracticeSessionDetailRepository or TestDetailRepository)
3. Repository executes native SQL query returning Object[]
4. Service maps Object[] to StudentPracticeProgressDTO
5. Controller returns DTO to client

## Proposed Changes

### 1. Enhanced DTO Structure

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentPracticeProgressDTO {
    // Main fields (Set 1 only OR Set 1 + Set 2 combined based on set1Complete)
    private Long totalQuestions;
    private Long practicedQuestions;
    private Long correctAnswers;
    private Long incorrectAnswers;
    private Long notPracticed;
    
    // Set 1 specific breakdown
    private Long set1Total;
    private Long set1Practiced;
    private Long set1Correct;
    private Long set1Incorrect;
    private Long set1NotPracticed;
    
    // Set 2 specific breakdown
    private Long set2Total;
    private Long set2Practiced;
    private Long set2Correct;
    private Long set2Incorrect;
    private Long set2NotPracticed;
    
    // Completion flag
    private Boolean set1Complete;
}
```

### 2. New Repository Methods

#### PracticeSessionDetailRepository

```java
// Get progress for a specific question set by chapter
@Query(value = """
    SELECT
        (SELECT COUNT(*)
         FROM questions q
         WHERE q.chapter_id = :chapterId
           AND q.question_set = :questionSet
           AND LOWER(COALESCE(q.usage_type, 'both')) IN ('practice', 'both')),
        
        (SELECT COUNT(DISTINCT psd.question_id)
         FROM practice_session_details psd
         JOIN questions q ON psd.question_id = q.question_id
         WHERE psd.student_id = :studentId
           AND q.chapter_id = :chapterId
           AND q.question_set = :questionSet
           AND LOWER(COALESCE(q.usage_type, 'both')) IN ('practice', 'both')),
        
        (SELECT COUNT(*)
         FROM practice_session_details psd
         JOIN questions q ON psd.question_id = q.question_id
         WHERE psd.student_id = :studentId
           AND q.chapter_id = :chapterId
           AND q.question_set = :questionSet
           AND psd.is_correct = true
           AND LOWER(COALESCE(q.usage_type, 'both')) IN ('practice', 'both')),
        
        (SELECT COUNT(*)
         FROM practice_session_details psd
         JOIN questions q ON psd.question_id = q.question_id
         WHERE psd.student_id = :studentId
           AND q.chapter_id = :chapterId
           AND q.question_set = :questionSet
           AND psd.is_correct = false
           AND LOWER(COALESCE(q.usage_type, 'both')) IN ('practice', 'both'))
    """, nativeQuery = true)
List<Object[]> getChapterProgressBySet(
    @Param("studentId") Long studentId, 
    @Param("chapterId") String chapterId,
    @Param("questionSet") String questionSet
);

// Get progress for a specific question set by subject
@Query(value = """
    SELECT
        (SELECT COUNT(*)
         FROM questions q
         JOIN chapters c ON q.chapter_id = c.chapter_id
         WHERE c.subject_id = :subjectId
           AND q.question_set = :questionSet
           AND LOWER(COALESCE(q.usage_type, 'both')) IN ('practice', 'both')),
        
        (SELECT COUNT(DISTINCT psd.question_id)
         FROM practice_session_details psd
         JOIN questions q ON psd.question_id = q.question_id
         JOIN chapters c ON q.chapter_id = c.chapter_id
         WHERE psd.student_id = :studentId
           AND c.subject_id = :subjectId
           AND q.question_set = :questionSet
           AND LOWER(COALESCE(q.usage_type, 'both')) IN ('practice', 'both')),
        
        (SELECT COUNT(*)
         FROM practice_session_details psd
         JOIN questions q ON psd.question_id = q.question_id
         JOIN chapters c ON q.chapter_id = c.chapter_id
         WHERE psd.student_id = :studentId
           AND c.subject_id = :subjectId
           AND q.question_set = :questionSet
           AND psd.is_correct = true
           AND LOWER(COALESCE(q.usage_type, 'both')) IN ('practice', 'both')),
        
        (SELECT COUNT(*)
         FROM practice_session_details psd
         JOIN questions q ON psd.question_id = q.question_id
         JOIN chapters c ON q.chapter_id = c.chapter_id
         WHERE psd.student_id = :studentId
           AND c.subject_id = :subjectId
           AND q.question_set = :questionSet
           AND psd.is_correct = false
           AND LOWER(COALESCE(q.usage_type, 'both')) IN ('practice', 'both'))
    """, nativeQuery = true)
List<Object[]> getSubjectProgressBySet(
    @Param("studentId") Long studentId, 
    @Param("subjectId") String subjectId,
    @Param("questionSet") String questionSet
);
```

#### TestDetailRepository

Similar methods for test progress:
- `getChapterProgressBySet(studentId, chapterId, questionSet)`
- `getSubjectProgressBySet(studentId, subjectId, questionSet)`

### 3. Service Layer Logic

#### StudentService Updates

```java
public StudentPracticeProgressDTO getPracticeProgressByChapter(Long studentId, String chapterId) {
    // Validate student exists
    studentRepository.findById(studentId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Student not found with ID: " + studentId));
    
    // Get Set 1 progress
    List<Object[]> set1Results = practiceSessionDetailRepository
        .getChapterProgressBySet(studentId, chapterId, "1");
    
    // Get Set 2 progress
    List<Object[]> set2Results = practiceSessionDetailRepository
        .getChapterProgressBySet(studentId, chapterId, "2");
    
    // Build and return DTO with set-aware logic
    return buildSetAwareProgressDTO(set1Results, set2Results);
}

private StudentPracticeProgressDTO buildSetAwareProgressDTO(
    List<Object[]> set1Results, 
    List<Object[]> set2Results
) {
    // Extract Set 1 data
    SetProgressData set1Data = extractSetProgressData(set1Results);
    
    // Extract Set 2 data
    SetProgressData set2Data = extractSetProgressData(set2Results);
    
    // Determine if Set 1 is complete
    boolean set1Complete = isSetComplete(set1Data);
    
    // Create DTO
    StudentPracticeProgressDTO dto = new StudentPracticeProgressDTO();
    
    // Set 1 breakdown (always populated)
    dto.setSet1Total(set1Data.total);
    dto.setSet1Practiced(set1Data.practiced);
    dto.setSet1Correct(set1Data.correct);
    dto.setSet1Incorrect(set1Data.incorrect);
    dto.setSet1NotPracticed(set1Data.total - set1Data.practiced);
    
    // Set 2 breakdown (always populated)
    dto.setSet2Total(set2Data.total);
    dto.setSet2Practiced(set2Data.practiced);
    dto.setSet2Correct(set2Data.correct);
    dto.setSet2Incorrect(set2Data.incorrect);
    dto.setSet2NotPracticed(set2Data.total - set2Data.practiced);
    
    // Set completion flag
    dto.setSet1Complete(set1Complete);
    
    // Main fields logic
    if (set1Complete) {
        // Show combined Set 1 + Set 2 totals
        dto.setTotalQuestions(set1Data.total + set2Data.total);
        dto.setPracticedQuestions(set1Data.practiced + set2Data.practiced);
        dto.setCorrectAnswers(set1Data.correct + set2Data.correct);
        dto.setIncorrectAnswers(set1Data.incorrect + set2Data.incorrect);
        dto.setNotPracticed(
            (set1Data.total + set2Data.total) - 
            (set1Data.practiced + set2Data.practiced)
        );
    } else {
        // Show only Set 1 totals
        dto.setTotalQuestions(set1Data.total);
        dto.setPracticedQuestions(set1Data.practiced);
        dto.setCorrectAnswers(set1Data.correct);
        dto.setIncorrectAnswers(set1Data.incorrect);
        dto.setNotPracticed(set1Data.total - set1Data.practiced);
    }
    
    return dto;
}

private boolean isSetComplete(SetProgressData setData) {
    // Set is complete when all questions have been practiced
    return setData.total > 0 && setData.practiced >= setData.total;
}

private SetProgressData extractSetProgressData(List<Object[]> results) {
    if (results == null || results.isEmpty()) {
        return new SetProgressData(0L, 0L, 0L, 0L);
    }
    
    Object[] row = results.get(0);
    Long total = row[0] != null ? ((Number) row[0]).longValue() : 0L;
    Long practiced = row[1] != null ? ((Number) row[1]).longValue() : 0L;
    Long correct = row[2] != null ? ((Number) row[2]).longValue() : 0L;
    Long incorrect = row[3] != null ? ((Number) row[3]).longValue() : 0L;
    
    return new SetProgressData(total, practiced, correct, incorrect);
}

// Helper class for set progress data
private static class SetProgressData {
    Long total;
    Long practiced;
    Long correct;
    Long incorrect;
    
    SetProgressData(Long total, Long practiced, Long correct, Long incorrect) {
        this.total = total;
        this.practiced = practiced;
        this.correct = correct;
        this.incorrect = incorrect;
    }
}
```

## Response Examples

### Example 1: Set 1 Incomplete (40% complete)

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

### Example 2: Set 1 Complete, Set 2 Not Started

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

### Example 3: Set 1 Complete, Set 2 In Progress

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

## Implementation Steps

1. ✅ **Analysis Phase**: Review current implementation
2. **DTO Enhancement**: Add set-specific fields to [`StudentPracticeProgressDTO`](src/main/java/com/ishine/ishinerest/pojo/StudentPracticeProgressDTO.java)
3. **Repository Updates**: Add set-specific query methods to:
   - [`PracticeSessionDetailRepository`](src/main/java/com/ishine/ishinerest/repository/PracticeSessionDetailRepository.java)
   - [`TestDetailRepository`](src/main/java/com/ishine/ishinerest/repository/TestDetailRepository.java)
4. **Service Logic**: Update [`StudentService`](src/main/java/com/ishine/ishinerest/service/StudentService.java) methods:
   - `getPracticeProgressByChapter()`
   - `getPracticeProgressBySubject()`
   - `getTestProgressByChapter()`
   - `getTestProgressBySubject()`
5. **Helper Methods**: Create utility methods for:
   - Extracting set progress data
   - Calculating set1Complete flag
   - Combining set totals
6. **Testing**: Verify all scenarios work correctly
7. **Documentation**: Update API documentation

## Testing Scenarios

### Scenario 1: Set 1 Not Started
- Set 1: 0/50 practiced
- Expected: Main fields show Set 1 only, set1Complete = false

### Scenario 2: Set 1 Partially Complete
- Set 1: 25/50 practiced
- Expected: Main fields show Set 1 only, set1Complete = false

### Scenario 3: Set 1 Just Completed
- Set 1: 50/50 practiced, Set 2: 0/45 practiced
- Expected: Main fields show combined (50+45=95 total), set1Complete = true

### Scenario 4: Both Sets In Progress
- Set 1: 50/50 practiced, Set 2: 20/45 practiced
- Expected: Main fields show combined totals, set1Complete = true

### Scenario 5: Both Sets Complete
- Set 1: 50/50 practiced, Set 2: 45/45 practiced
- Expected: Main fields show combined totals, set1Complete = true

## Backward Compatibility

- Existing API endpoints remain unchanged (no new parameters required)
- Response structure is enhanced (adds new fields, doesn't remove existing ones)
- Frontend can gradually adopt new fields
- Old clients continue to work with main fields

## Database Considerations

- No schema changes required
- Leverages existing `question_set` column in questions table
- Queries filter by `question_set = '1'` and `question_set = '2'`

## Performance Considerations

- Two separate queries per request (one for Set 1, one for Set 2)
- Queries are optimized with existing indexes
- Minimal performance impact (queries are already fast)
- Consider caching if needed in future

## Future Enhancements

- Support for Set 3, Set 4, etc. (if needed)
- Progress history tracking
- Set-specific analytics
- Personalized set recommendations

---

**Created**: 2026-05-23  
**Author**: Bob (Plan Mode)  
**Status**: Ready for Implementation