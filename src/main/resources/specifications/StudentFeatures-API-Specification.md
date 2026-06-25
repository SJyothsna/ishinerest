# Student Features API Specification

## Overview
Student-specific features including question flagging for review and parent link request management.

---

## FlaggedQuestionController

### Base URL
`/flagged-questions`

### Overview
Allows students to flag/bookmark questions for later review and retrieve flagged questions.

---

### Endpoints

#### 1. Flag a Question
**POST** `/flagged-questions/student/{studentId}/question/{questionId}`

Flags a question for a student.

**Path Parameters:**
- `studentId` (required): Student ID
- `questionId` (required): Question ID

**Response (201 Created):**
```json
{
  "id": 1,
  "studentId": 1,
  "questionId": 15,
  "flaggedAt": "2024-01-22T10:30:00Z"
}
```

---

#### 2. Unflag a Question
**DELETE** `/flagged-questions/student/{studentId}/question/{questionId}`

Removes flag from a question.

**Response (200 OK):** Empty

---

#### 3. Get Flagged Questions by Chapter
**GET** `/flagged-questions/student/{studentId}/chapter/{chapterId}`

Retrieves all flagged questions for a student in a specific chapter.

**Response (200 OK):**
```json
[
  {
    "id": 15,
    "sectionId": "LC5H0101-S1",
    "questionText": "What is 2 + 2?",
    "optionA": "3",
    "optionB": "4",
    "optionC": "5",
    "optionD": "6",
    "correctAnswer": "B",
    "isFlagged": true,
    "flaggedAt": "2024-01-22T10:30:00Z"
  }
]
```

---

#### 4. Get All Flagged Questions
**GET** `/flagged-questions/student/{studentId}`

Retrieves all flagged questions for a student across all chapters.

**Response (200 OK):** Array of QuestionWithFlagDTO

---

#### 5. Get Unpracticed Flagged Questions by Chapter
**GET** `/flagged-questions/student/{studentId}/chapter/{chapterId}/unpracticed`

Gets flagged questions that haven't been correctly answered yet.

**Response (200 OK):** Array of QuestionWithFlagDTO

---

## StudentLinkController

### Base URL
`/students`

### Overview
Manages student approval/rejection of parent link requests.

---

### Endpoints

#### 1. Get Pending Link Requests
**GET** `/students/{studentUserId}/link-requests`

Retrieves all pending parent link requests for a student.

**Path Parameters:**
- `studentUserId` (required): Student's user ID

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "parentUserId": 10,
    "parentName": "Jane Doe",
    "parentEmail": "jane.doe@example.com",
    "studentUserId": 15,
    "studentName": "John Doe",
    "studentEmail": "john.doe@example.com",
    "relationshipType": "PARENT",
    "status": "PENDING",
    "requestedAt": "2024-01-20T10:30:00Z",
    "respondedAt": null
  }
]
```

---

#### 2. Approve Link Request
**POST** `/students/link-requests/{parentUserId}/approve`

Approves a parent's link request.

**Path Parameters:**
- `parentUserId` (required): Parent's user ID

**Request Body:**
```json
{
  "studentUserId": 15
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "parentUserId": 10,
  "parentName": "Jane Doe",
  "parentEmail": "jane.doe@example.com",
  "studentUserId": 15,
  "studentName": "John Doe",
  "studentEmail": "john.doe@example.com",
  "relationshipType": "PARENT",
  "status": "ACTIVE",
  "requestedAt": "2024-01-20T10:30:00Z",
  "respondedAt": "2024-01-20T11:00:00Z"
}
```

---

#### 3. Reject Link Request
**POST** `/students/link-requests/{parentUserId}/reject`

Rejects a parent's link request.

**Path Parameters:**
- `parentUserId` (required): Parent's user ID

**Request Body:**
```json
{
  "studentUserId": 15
}
```

**Response (204 No Content):** Empty

---

## Data Models

### FlaggedQuestion
```json
{
  "id": "long",
  "studentId": "long",
  "questionId": "long",
  "flaggedAt": "datetime (ISO 8601)"
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
  "isFlagged": "boolean",
  "flaggedAt": "datetime (optional)"
}
```

### ParentStudentLinkDTO
```json
{
  "id": "long",
  "parentUserId": "long",
  "parentName": "string",
  "parentEmail": "string",
  "studentUserId": "long",
  "studentName": "string",
  "studentEmail": "string",
  "relationshipType": "enum (PARENT, GUARDIAN, OTHER)",
  "status": "enum (PENDING, ACTIVE, REJECTED, REVOKED)",
  "requestedAt": "datetime",
  "respondedAt": "datetime (nullable)"
}
```

---

## Notes for UI Team

### Flagged Questions Feature

#### Use Cases
1. **During Practice**: Student flags difficult questions
2. **Review Session**: Student reviews all flagged questions
3. **Focused Practice**: Practice only unpracticed flagged questions

#### UI Implementation
- Show flag icon on each question
- Toggle flag on/off with single click
- Show flag count in chapter/subject overview
- Create "Flagged Questions" section in student dashboard

#### Flag Icon States
- **Unflagged**: Empty bookmark icon
- **Flagged**: Filled bookmark icon (yellow/gold color)
- **Hover**: Show tooltip "Flag for review"

#### Flagged Questions View
```
My Flagged Questions
├── Mathematics (15 questions)
│   ├── Chapter 1: Number Systems (5)
│   └── Chapter 2: Algebra (10)
└── Science (8 questions)
    └── Chapter 1: Physics (8)
```

#### Review Workflow
1. Student goes to "Flagged Questions" section
2. Filter by subject/chapter
3. Practice flagged questions
4. Auto-unflag when answered correctly (optional)
5. Manual unflag option always available

#### Performance Tips
- Cache flag status locally
- Batch flag/unflag operations
- Show optimistic UI updates
- Sync with server in background

---

### Parent Link Requests Feature

#### Student Notification Flow
1. Parent requests link via email
2. Student receives notification (implement notification system)
3. Student sees pending request in dashboard
4. Student approves or rejects

#### UI Components

**Pending Requests Badge**
```
Notifications (1)
└── Parent Link Request from Jane Doe
```

**Request Card**
```
┌─────────────────────────────────────┐
│ Parent Link Request                  │
├─────────────────────────────────────┤
│ Jane Doe (jane.doe@example.com)     │
│ wants to link as your Parent        │
│                                      │
│ Requested: Jan 20, 2024             │
│                                      │
│ [Approve] [Reject]                  │
└─────────────────────────────────────┘
```

#### Approval Dialog
```
Approve Parent Link Request?

Jane Doe will be able to:
✓ View your progress and scores
✓ See your practice history
✓ Create custom tests for you
✓ View your notes and flagged questions

[Cancel] [Approve]
```

#### Rejection Confirmation
```
Reject Link Request?

Are you sure you want to reject the link
request from Jane Doe?

[Cancel] [Reject]
```

#### Status Indicators
- **PENDING**: Yellow badge with "Pending Approval"
- **ACTIVE**: Green checkmark with "Connected"
- **REJECTED**: Red X with "Declined"

#### Security Considerations
- Verify student identity before showing requests
- Show parent's email for verification
- Allow student to revoke access later
- Log all approval/rejection actions

#### Notification System
- Email notification when request received
- In-app notification badge
- Push notification (if mobile app)
- Reminder after 3 days if not responded

#### Privacy Settings
- Allow student to control what parents can see
- Option to hide specific subjects/chapters
- Option to disable parent link requests

---

### Integration Points

#### Flagged Questions + Practice Mode
```javascript
// Get unpracticed flagged questions for focused review
GET /flagged-questions/student/{studentId}/chapter/{chapterId}/unpracticed

// During practice, show flag status
GET /questions/unpracticed/chapter?studentId=1&chapterId=LC5H0101
// Response includes isFlagged field

// Toggle flag during practice
POST /flagged-questions/student/1/question/15
DELETE /flagged-questions/student/1/question/15
```

#### Parent Links + Dashboard
```javascript
// Check for pending requests on login
GET /students/{studentUserId}/link-requests

// Show notification if count > 0
if (pendingRequests.length > 0) {
  showNotificationBadge(pendingRequests.length);
}

// Handle approval
POST /students/link-requests/{parentUserId}/approve
// Then refresh parent list
GET /parents/students/{studentUserId}/parents
```

---

### Error Handling

#### Flagged Questions
- **404**: Question or student not found
- **409**: Question already flagged (ignore or show message)
- **400**: Invalid student or question ID

#### Parent Links
- **404**: Request not found or already processed
- **400**: Invalid request data
- **409**: Request already approved/rejected

### Best Practices

1. **Optimistic UI**: Update UI immediately, sync with server
2. **Offline Support**: Queue flag operations when offline
3. **Batch Operations**: Group multiple flag/unflag operations
4. **Cache Management**: Cache flagged question IDs locally
5. **Real-time Updates**: Use WebSocket for link request notifications