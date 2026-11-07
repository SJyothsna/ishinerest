# iShine — Project Overview

## What is iShine?

iShine is a backend REST API for a student study app targeting real schools and students in Ireland.
It is a startup/side project currently in pre-production. The app lets students practice exam questions,
take teacher-assigned tests, and track their own progress — while giving teachers, parents, and admins
the tools to support and monitor them.

**Tech stack:**
- Java 17, Spring Boot 3.2.2
- Spring Data JPA + Hibernate
- H2 (local/dev, file-based at `./data/testdb`) — MySQL-ready for production
- Spring Mail (Gmail SMTP) for email verification and password reset
- SpringDoc / Swagger UI at `/swagger-ui.html`
- Lombok for boilerplate reduction
- `.http` test files under `src/test/java/rest/` per feature area

---

## Main Goals and Objectives

1. Provide a secure, role-based REST API consumed by a frontend (React, likely at port 3000 or 5173).
2. Let students practice questions organised by subject → chapter → section → question set.
3. Let teachers create custom tests and assign them to students with due dates and feedback.
4. Let parents monitor their child's progress (with student approval of the parent link).
5. Let admins manage content: questions, subjects, chapters, classes.
6. Track practice session results and test attempt history per student.

---

## User Roles

| Role | Responsibilities |
|------|-----------------|
| **STUDENT** | Register, browse questions by subject/chapter, submit practice sessions, take assigned tests, manage notes, flag questions |
| **TEACHER** | Create and publish tests, assign tests to students, view student results, manage subject links |
| **PARENT** | Link to a student (pending student approval), monitor progress |
| **ADMIN** | Manage questions, subjects, chapters, classes, users |

Role is stored in the `users` table as an enum: `STUDENT | TEACHER | PARENT | ADMIN`.

---

## Key Modules

### Authentication (`/auth`)
- Signup, login, email verification, forgot-password, reset-password
- Token returned on login as a plain string (no JWT filter enforced yet)
- Email sent via Gmail SMTP; credentials in `application.properties` (not yet in env vars)
- CORS whitelisted for `localhost:3000` and `localhost:5173`

### Content Hierarchy
```
ClassEntity (e.g. "5th Year")
  └── SubjectEntity (e.g. "Maths") — via class_subjects join table
        └── Chapter (e.g. "Algebra") — chapterId is manually assigned string
              └── Question — belongs to chapter; has sectionId, questionSet, usageType
```

### Questions (`/questions`)
- MCQ with up to 6 options (A–F), single or multi-correct
- Fields: `questionText`, `optionA-F`, `correctAnswer`, `correctAnswers`, `explanation`, `hint`, `notes`, `tags`
- `usageType`: `Practice`, `Test`, or `Both`
- `questionSet`: progressive difficulty bands (1, 2, 3…)
- `isCustom` + `visibility` (PUBLIC / PRIVATE / SHARED): supports teacher-created custom questions
- `questionImageUrl`: optional image URL per question

### Practice Sessions (`/practice-session-details`)
- Each submitted answer creates a `PracticeSessionDetail` record
- Tracks `studentAnswer`, `isCorrect`, `attemptCount` per student per question
- `subjectId` and `chapterId` are transient (not persisted, used for routing only)

### Tests
- **TeacherTest**: created by a teacher (`teacher_tests` table), with title, description, subject, chapter, duration, `isPublished`
- **TeacherTestQuestion**: join table linking a `TeacherTest` to specific `Question` rows
- **TestAssignment**: assigns a test to a specific student with `status` lifecycle: `ASSIGNED → IN_PROGRESS → COMPLETED / OVERDUE`, plus `score`, `dueDate`, `feedback`
- **TestDetail**: individual question-answer records for a test attempt (mirrors PracticeSessionDetail for test context)

### Relationships
- **teacher_student**: teacher ↔ student link (with optional `subjectId`)
- **parent_student**: parent ↔ student link with `LinkStatus` lifecycle: `PENDING → ACTIVE / REJECTED / REVOKED`
- **student_subjects**: student's selected subjects
- **teacher_subjects**: teacher's selected subjects

### Notes & Extras
- **StudentNote**: private notes per student per question/chapter
- **CommonNote**: admin-created shared notes
- **FlaggedQuestion**: student flags a question for review
- **Feedback**: general user feedback submission
- **PrevExamPaper / PrevExamQuestion**: past exam papers and their questions (separate from the main question bank)

---

## Database Tables (summary)

| Table | Purpose |
|-------|---------|
| `users` | All user accounts (all roles) |
| `students` | Student profile, FK = user_id (1:1 with users) |
| `classes` | School year / class groups |
| `subjects` | Subjects, linked to classes via `class_subjects` |
| `chapters` | Chapters per subject |
| `questions` | Question bank |
| `practice_session_details` | Student practice attempt records |
| `teacher_tests` | Teacher-authored tests |
| `teacher_test_questions` | Questions within a teacher test |
| `test_assignments` | Test assigned to a student |
| `test_details` | Per-question answer records for a test |
| `teacher_student` | Teacher–student links |
| `parent_student` | Parent–student links |
| `student_subjects` | Student's chosen subjects |
| `teacher_subjects` | Teacher's chosen subjects |
| `student_notes` | Per-student private notes |
| `common_notes` | Admin shared notes |
| `flagged_questions` | Student-flagged questions |
| `feedback` | App feedback submissions |
| `prev_exam_papers` | Past exam papers |
| `prev_exam_questions` | Questions in past papers |
| `password_reset_tokens` | Tokens for password reset flow |

---

## API Controllers (surface area)

| Controller | Base path | Description |
|------------|-----------|-------------|
| AuthController | `/auth` | Signup, login, email verify, password reset |
| StudentController | `/students` | Student profile, subjects, progress |
| TeacherController | `/teachers` | Teacher profile, students, subjects |
| ParentController | `/parents` | Parent-student link management |
| AdminController | `/admin` | User/content admin operations |
| QuestionController | `/questions` | CRUD for questions |
| ChapterController | `/chapters` | Chapter management |
| SubjectController | `/subjects` | Subject management |
| ClassController | `/classes` | Class management |
| QuizController | `/quiz` | Quiz/practice session orchestration |
| PracticeSessionDetailController | `/practice-session-details` | Submit/retrieve practice results |
| TeacherTestController | `/teacher-tests` | Create/manage teacher tests |
| TestAssignmentController | `/test-assignments` | Assign tests to students |
| TestDetailController | `/test-details` | Store test attempt answers |
| PublicTestController | `/public-tests` | Publicly accessible test endpoints |
| StudentNoteController | `/student-notes` | Student private notes |
| CommonNoteController | `/common-notes` | Admin shared notes |
| FlaggedQuestionController | `/flagged-questions` | Flag/unflag questions |
| FeedbackController | `/feedback` | Submit feedback |
| PrevExamPaperController | `/prev-exam-papers` | Past exam papers |
| PrevExamQuestionController | `/prev-exam-questions` | Questions in past papers |
| ExamQuestionController | `/exam-questions` | Exam question management |
| QuestionImageController | `/question-images` | Question image upload |
| ChapterImageController | `/chapter-images` | Chapter image upload |
| StudentLinkController | `/student-links` | Teacher-student link management |
| UserController | `/users` | User profile updates |
| SectionController | `/sections` | Section management |
| HomeController | `/` | Home / health check |

---

## Important Notes

- **No Spring Security JWT filter** is currently in place — the token returned on login is not validated on subsequent requests by a filter. This is a known security gap to address before production.
- **CORS** is hardcoded per controller with `@CrossOrigin` annotations — not centralised in `WebConfig`.
- Email credentials are stored in plain text in `application.properties` — must be moved to environment variables before deployment.
- The H2 database is file-based locally; MySQL DDL scripts are provided as standalone SQL migration files in the root.
- Swagger UI is available at `/swagger-ui.html` for local testing.
