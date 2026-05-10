# Parent Feature - API Implementation Documentation

## Overview
This document describes the implemented parent-student linking feature with pending/approval workflow.

---

## Database Changes

### Migration File
`database_migration_add_parent_student_status.sql`

### New Columns in `parent_student` Table
- `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING'
- `updated_at` TIMESTAMP NOT NULL
- `approved_at` TIMESTAMP NULL
- `rejected_at` TIMESTAMP NULL

### Link Status Enum
```java
public enum LinkStatus {
    PENDING,   // Waiting for student approval
    ACTIVE,    // Approved and active
    REJECTED,  // Student rejected the request
    REVOKED    // Link was removed by parent or student
}
```

---

## API Endpoints

### 1. Link Student to Parent by Email

**Endpoint:** `POST /api/parents/{parentUserId}/link-student`

**Description:** Creates a pending link request from parent to student using email. Student must approve before link becomes active.

**Request Body:**
```json
{
  "studentEmail": "student@example.com",
  "relationshipType": "MOTHER"
}
```

**Success Response (201 Created):**
```json
{
  "parentUserId": 1,
  "parentName": "Jane Smith",
  "parentEmail": "jane@example.com",
  "studentUserId": 5,
  "studentName": "John Doe",
  "studentEmail": "student@example.com",
  "relationshipType": "MOTHER",
  "status": "PENDING",
  "createdAt": "2026-05-08T19:30:00",
  "approvedAt": null,
  "rejectedAt": null
}
```

**Error Responses:**
- `404 NOT_FOUND` - Student not found with this email
- `409 CONFLICT` - Link already exists or pending request exists
- `400 BAD_REQUEST` - Invalid email format or user is not a parent

---

### 2. Link Student to Parent by ID (Backward Compatibility)

**Endpoint:** `POST /api/parents/{parentUserId}/students`

**Description:** Creates a pending link request using student user ID.

**Request Body:**
```json
{
  "studentUserId": 5
}
```

**Success Response:** `201 CREATED` (no body)

---

### 3. Get Linked Students for Parent

**Endpoint:** `GET /api/parents/{parentUserId}/students`

**Description:** Returns all actively linked students for a parent.

**Success Response (200 OK):**
```json
[
  {
    "userId": 5,
    "name": "John Doe",
    "email": "student@example.com",
    "role": "STUDENT",
    "isActive": true,
    "createdAt": "2026-01-01T10:00:00"
  }
]
```

---

### 4. Get All Links for Parent (with Status Filter)

**Endpoint:** `GET /api/parents/{parentUserId}/links?status=PENDING`

**Description:** Returns all parent-student links with optional status filter.

**Query Parameters:**
- `status` (optional): PENDING, ACTIVE, REJECTED, or REVOKED

**Success Response (200 OK):**
```json
[
  {
    "parentUserId": 1,
    "parentName": "Jane Smith",
    "parentEmail": "jane@example.com",
    "studentUserId": 5,
    "studentName": "John Doe",
    "studentEmail": "student@example.com",
    "relationshipType": "MOTHER",
    "status": "PENDING",
    "createdAt": "2026-05-08T19:30:00",
    "approvedAt": null,
    "rejectedAt": null
  }
]
```

---

### 5. Unlink Student from Parent

**Endpoint:** `DELETE /api/parents/{parentUserId}/students/{studentUserId}`

**Description:** Revokes the parent-student link (marks as REVOKED, doesn't delete).

**Success Response:** `204 NO_CONTENT`

**Error Response:**
- `404 NOT_FOUND` - Link not found

---

### 6. Get Pending Link Requests for Student

**Endpoint:** `GET /api/students/{studentUserId}/link-requests`

**Description:** Returns all pending parent link requests for a student.

**Success Response (200 OK):**
```json
[
  {
    "parentUserId": 1,
    "parentName": "Jane Smith",
    "parentEmail": "jane@example.com",
    "studentUserId": 5,
    "studentName": "John Doe",
    "studentEmail": "student@example.com",
    "relationshipType": "MOTHER",
    "status": "PENDING",
    "createdAt": "2026-05-08T19:30:00",
    "approvedAt": null,
    "rejectedAt": null
  }
]
```

---

### 7. Approve Parent Link Request

**Endpoint:** `POST /api/students/link-requests/{parentUserId}/approve`

**Description:** Student approves a parent link request.

**Request Body:**
```json
{
  "studentUserId": 5
}
```

**Success Response (200 OK):**
```json
{
  "parentUserId": 1,
  "parentName": "Jane Smith",
  "parentEmail": "jane@example.com",
  "studentUserId": 5,
  "studentName": "John Doe",
  "studentEmail": "student@example.com",
  "relationshipType": "MOTHER",
  "status": "ACTIVE",
  "createdAt": "2026-05-08T19:30:00",
  "approvedAt": "2026-05-08T19:35:00",
  "rejectedAt": null
}
```

**Error Responses:**
- `404 NOT_FOUND` - Link request not found
- `400 BAD_REQUEST` - Request already processed

---

### 8. Reject Parent Link Request

**Endpoint:** `POST /api/students/link-requests/{parentUserId}/reject`

**Description:** Student rejects a parent link request.

**Request Body:**
```json
{
  "studentUserId": 5
}
```

**Success Response:** `204 NO_CONTENT`

**Error Responses:**
- `404 NOT_FOUND` - Link request not found
- `400 BAD_REQUEST` - Request already processed

---

### 9. Get Parents for Student

**Endpoint:** `GET /api/students/{studentUserId}/parents`

**Description:** Returns all actively linked parents for a student.

**Success Response (200 OK):**
```json
[
  {
    "userId": 1,
    "name": "Jane Smith",
    "email": "jane@example.com",
    "role": "PARENT",
    "isActive": true,
    "createdAt": "2026-01-01T10:00:00"
  }
]
```

---

### 10. Check if Parent is Linked to Student

**Endpoint:** `GET /api/parents/{parentUserId}/students/{studentUserId}/linked`

**Description:** Returns boolean indicating if parent is actively linked to student.

**Success Response (200 OK):**
```json
true
```

---

## Implementation Details

### New/Modified Files

**Entities:**
- `ParentStudent.java` - Added status, approvedAt, rejectedAt fields and LinkStatus enum

**DTOs:**
- `LinkStudentByEmailRequest.java` - Request DTO for email-based linking
- `ParentStudentLinkDTO.java` - Response DTO with full link information

**Repositories:**
- `ParentStudentRepository.java` - Added status-based query methods

**Services:**
- `ParentService.java` - Added approval workflow methods:
  - `linkParentToStudentByEmail()`
  - `approveLinkRequest()`
  - `rejectLinkRequest()`
  - `getPendingLinkRequests()`
  - Updated existing methods to filter by status

**Controllers:**
- `ParentController.java` - Added new endpoints for email-based linking and link management
- `StudentLinkController.java` - New controller for student approval workflow

---

## Workflow

### Parent Initiates Link
1. Parent calls `POST /api/parents/{parentUserId}/link-student` with student email
2. System creates link with `PENDING` status
3. Student receives notification (to be implemented)

### Student Responds
1. Student calls `GET /api/students/{studentUserId}/link-requests` to see pending requests
2. Student approves: `POST /api/students/link-requests/{parentUserId}/approve`
   - Status changes to `ACTIVE`
   - `approvedAt` timestamp set
3. OR Student rejects: `POST /api/students/link-requests/{parentUserId}/reject`
   - Status changes to `REJECTED`
   - `rejectedAt` timestamp set

### Parent Views Students
1. Parent calls `GET /api/parents/{parentUserId}/students` - sees only ACTIVE links
2. Parent calls `GET /api/parents/{parentUserId}/links?status=PENDING` - sees pending requests

### Revoke Access
1. Either parent or student can call `DELETE /api/parents/{parentUserId}/students/{studentUserId}`
2. Status changes to `REVOKED` (soft delete)

---

## Security Considerations

1. **Authentication Required:** All endpoints require valid JWT tokens
2. **Authorization:** Users can only access their own data
3. **Email Verification:** Both parent and student emails should be verified
4. **Rate Limiting:** Consider implementing rate limiting on link request endpoints
5. **Privacy:** Students control who can view their data through approval workflow

---

## Migration Instructions

1. **Backup Database:** Always backup before running migrations
2. **Run Migration:** Execute `database_migration_add_parent_student_status.sql`
3. **Verify:** Check that all existing links are marked as `ACTIVE`
4. **Deploy:** Deploy new application version
5. **Test:** Verify all endpoints work correctly

---

## Testing Checklist

- [x] Compilation successful
- [ ] Parent can send link request with valid student email
- [ ] Error shown when student email doesn't exist
- [ ] Error shown when link already exists
- [ ] Student can view pending requests
- [ ] Student can approve link request
- [ ] Student can reject link request
- [ ] Parent sees only active students by default
- [ ] Parent can filter links by status
- [ ] Parent can revoke access
- [ ] Student can revoke parent access
- [ ] Multiple parents can link to same student

---

## Future Enhancements

1. **Real-time Notifications:** WebSocket notifications for link requests
2. **Progress Tracking:** APIs for viewing student progress
3. **Privacy Settings:** Student-configurable privacy levels
4. **Email Notifications:** Send emails for link requests and approvals
5. **Relationship Types:** Expand relationship type options
6. **Bulk Operations:** Link multiple students at once

---

## Build Status

✅ **Compilation Successful**
- All files compile without errors
- 120 source files compiled
- Build time: ~16 seconds

---

*Documentation generated: 2026-05-08*
*Made with Bob*