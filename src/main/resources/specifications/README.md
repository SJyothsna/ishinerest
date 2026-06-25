# iShine REST API Specifications

## Overview
This directory contains comprehensive API specifications for all controllers in the iShine REST application. Each specification includes endpoint details, request/response examples, and UI implementation notes.

---

## Available Specifications

### ✅ Completed Specifications

1. **[AuthController-API-Specification.md](./AuthController-API-Specification.md)**
   - User authentication and registration
   - Password reset and email verification
   - Base URL: `/auth`

2. **[ChapterController-API-Specification.md](./ChapterController-API-Specification.md)**
   - Chapter management and CRUD operations
   - Bulk uploads from Excel
   - Base URL: `/chapters`

3. **[ClassController-API-Specification.md](./ClassController-API-Specification.md)**
   - Class/grade level management
   - Base URL: `/classes`

4. **[SubjectController-API-Specification.md](./SubjectController-API-Specification.md)**
   - Subject management within classes
   - Base URL: `/subjects`

5. **[UserController-API-Specification.md](./UserController-API-Specification.md)**
   - User profile and account management
   - Password changes and account status
   - Base URL: `/users`

6. **[StudentController-API-Specification.md](./StudentController-API-Specification.md)**
   - Student profiles and subject selection
   - Practice and test progress tracking
   - Base URL: `/students`

7. **[TeacherController-API-Specification.md](./TeacherController-API-Specification.md)**
   - Teacher-student relationship management
   - Teacher subject assignments
   - Base URL: `/teachers`

8. **[ParentController-API-Specification.md](./ParentController-API-Specification.md)**
   - Parent-student link requests with approval workflow
   - Base URL: `/parents`

---

## Pending Specifications

The following controllers need specifications to be created:

### Question & Practice Management
- **QuestionController** - Question bank management, custom questions, practice questions
- **QuestionImageController** - Question image uploads
- **FlaggedQuestionController** - Student question flagging for review
- **ExamQuestionController** - Exam-specific questions
- **PracticeSessionDetailController** - Practice session tracking

### Test Management
- **TeacherTestController** - Test creation and assignment (teachers/parents)
- **TestAssignmentController** - Student test assignments
- **TestDetailController** - Test attempt details and results
- **PublicTestController** - Public test access

### Notes & Content
- **StudentNoteController** - Student personal notes
- **CommonNoteController** - Shared notes and resources
- **ChapterImageController** - Chapter-related images

### Previous Exams
- **PrevExamPaperController** - Previous exam paper management
- **PrevExamQuestionController** - Previous exam questions

### Student Links
- **StudentLinkController** - Student approval of parent link requests

---

## API Structure

### Base URL
All APIs are accessible at: `http://localhost:8080` (development)

### Common Response Codes
- **200 OK**: Successful GET/PUT/DELETE request
- **201 Created**: Successful POST request creating a resource
- **204 No Content**: Successful request with no response body
- **400 Bad Request**: Invalid input or validation error
- **401 Unauthorized**: Authentication required
- **403 Forbidden**: Insufficient permissions
- **404 Not Found**: Resource not found
- **409 Conflict**: Duplicate resource or constraint violation
- **500 Internal Server Error**: Server-side error

### CORS Configuration
Most controllers allow requests from:
- `http://localhost:3000` (React dev server)
- `http://localhost:5173` (Vite dev server)

Credentials are enabled for cross-origin requests.

---

## Data Hierarchy

Understanding the data relationships:

```
User (STUDENT, TEACHER, PARENT, ADMIN)
  └─ Student
      ├─ Class
      ├─ Subjects (selected)
      ├─ Teachers (linked)
      ├─ Parents (linked with approval)
      ├─ Practice Sessions
      ├─ Test Assignments
      ├─ Flagged Questions
      └─ Notes

Class
  └─ Subjects
      └─ Chapters
          └─ Questions
              ├─ Practice Questions
              ├─ Exam Questions
              └─ Custom Questions

Teacher/Parent
  ├─ Students (linked)
  ├─ Subjects (teaching)
  └─ Tests (created)
      └─ Assignments (to students)
```

---

## Authentication & Authorization

### User Roles
- **STUDENT**: Can practice, take tests, manage notes
- **TEACHER**: Can manage students, create tests, view progress
- **PARENT**: Can view linked children's progress, create custom tests
- **ADMIN**: Full system access

### Token-Based Authentication
Most endpoints require authentication via JWT tokens obtained from the `/auth/login` endpoint.

---

## Common Patterns

### Pagination
Currently not implemented. All list endpoints return complete datasets.

### Filtering
Many endpoints support query parameters for filtering:
- `?subjectId=LC5H01` - Filter by subject
- `?chapterId=LC5H0101` - Filter by chapter
- `?studentId=1` - Filter by student
- `?status=ACTIVE` - Filter by status

### Bulk Operations
Several endpoints support bulk operations:
- Creating multiple chapters
- Creating multiple questions
- Assigning tests to multiple students

### File Uploads
File upload endpoints use `multipart/form-data`:
- Excel uploads for bulk data import
- Image uploads for questions and notes

---

## UI Implementation Guidelines

### Error Handling
Always implement proper error handling:
```javascript
try {
  const response = await fetch('/api/endpoint');
  if (!response.ok) {
    const error = await response.json();
    // Show user-friendly error message
  }
} catch (error) {
  // Handle network errors
}
```

### Loading States
Show loading indicators during API calls to improve UX.

### Optimistic Updates
Consider optimistic UI updates for better perceived performance, with rollback on error.

### Caching
Cache frequently accessed data (classes, subjects, chapters) to reduce API calls.

### Real-time Updates
Consider implementing WebSocket connections for real-time progress updates and notifications.

---

## Testing

### Test Files Location
HTTP test files are available in: `src/test/java/rest/`

Each controller has corresponding test files with sample requests.

---

## Support

For questions or issues with the API specifications, contact the development team.

---

**Last Updated**: 2024-01-22
**API Version**: 1.0