# Parent API Specification

## Base URL
`/parents`

## Overview
Manages parent-student relationships with approval workflow, allowing parents to request access to student accounts and students to approve/reject these requests.

---

## Endpoints

### 1. Link Parent to Student by Email
**POST** `/parents/{parentUserId}/link-student`

Creates a pending link request from parent to student using student's email.

#### Path Parameters
- `parentUserId` (required): The parent's user ID

#### Request Body
```json
{
  "studentEmail": "john.doe@example.com",
  "relationshipType": "PARENT"
}
```

#### Response (201 Created)
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
  "status": "PENDING",
  "requestedAt": "2024-01-20T10:30:00Z",
  "respondedAt": null
}
```

#### Relationship Types
- `PARENT`: Biological parent
- `GUARDIAN`: Legal guardian
- `OTHER`: Other relationship

#### Link Status
- `PENDING`: Awaiting student approval
- `ACTIVE`: Approved and active
- `REJECTED`: Rejected by student
- `REVOKED`: Access revoked by parent or student

---

### 2. Link Parent to Student by ID
**POST** `/parents/{parentUserId}/students`

Directly links a parent to a student (backward compatibility, auto-approved).

#### Path Parameters
- `parentUserId` (required): The parent's user ID

#### Request Body
```json
{
  "studentUserId": 15
}
```

#### Response (201 Created)
```
(Empty response body)
```

---

### 3. Unlink Parent from Student
**DELETE** `/parents/{parentUserId}/students/{studentUserId}`

Revokes parent's access to a student account.

#### Path Parameters
- `parentUserId` (required): The parent's user ID
- `studentUserId` (required): The student's user ID

#### Response (204 No Content)
```
(Empty response body)
```

---

### 4. Get Students for Parent
**GET** `/parents/{parentUserId}/students`

Retrieves all students with active links to a parent.

#### Path Parameters
- `parentUserId` (required): The parent's user ID

#### Response (200 OK)
```json
[
  {
    "userId": 15,
    "name": "John Doe",
    "email": "john.doe@example.com",
    "role": "STUDENT",
    "isActive": true,
    "emailVerified": true,
    "createdAt": "2024-01-15T10:30:00Z"
  },
  {
    "userId": 16,
    "name": "Jane Doe Jr.",
    "email": "jane.jr@example.com",
    "role": "STUDENT",
    "isActive": true,
    "emailVerified": true,
    "createdAt": "2024-01-16T09:15:00Z"
  }
]
```

---

### 5. Get All Links for Parent
**GET** `/parents/{parentUserId}/links?status={status}`

Retrieves all parent-student links with optional status filter.

#### Path Parameters
- `parentUserId` (required): The parent's user ID

#### Query Parameters
- `status` (optional): Filter by link status (PENDING, ACTIVE, REJECTED, REVOKED)

#### Response (200 OK)
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
    "status": "ACTIVE",
    "requestedAt": "2024-01-20T10:30:00Z",
    "respondedAt": "2024-01-20T11:00:00Z"
  },
  {
    "id": 2,
    "parentUserId": 10,
    "parentName": "Jane Doe",
    "parentEmail": "jane.doe@example.com",
    "studentUserId": 16,
    "studentName": "Jane Doe Jr.",
    "studentEmail": "jane.jr@example.com",
    "relationshipType": "PARENT",
    "status": "PENDING",
    "requestedAt": "2024-01-21T09:00:00Z",
    "respondedAt": null
  }
]
```

#### Example Usage
- Get all links: `GET /parents/10/links`
- Get pending requests: `GET /parents/10/links?status=PENDING`
- Get active links: `GET /parents/10/links?status=ACTIVE`

---

### 6. Get Parents for Student
**GET** `/parents/students/{studentUserId}/parents`

Retrieves all parents with active links to a student.

#### Path Parameters
- `studentUserId` (required): The student's user ID

#### Response (200 OK)
```json
[
  {
    "userId": 10,
    "name": "Jane Doe",
    "email": "jane.doe@example.com",
    "role": "PARENT",
    "isActive": true,
    "emailVerified": true,
    "createdAt": "2024-01-10T08:00:00Z"
  }
]
```

---

### 7. Check if Parent is Linked to Student
**GET** `/parents/{parentUserId}/students/{studentUserId}/linked`

Checks if a parent has active access to a student account.

#### Path Parameters
- `parentUserId` (required): The parent's user ID
- `studentUserId` (required): The student's user ID

#### Response (200 OK)
```json
true
```

---

## Data Models

### LinkStudentByEmailRequest
```json
{
  "studentEmail": "string (required, valid email format)",
  "relationshipType": "enum (PARENT, GUARDIAN, OTHER)"
}
```

### LinkStudentRequest
```json
{
  "studentUserId": "long (required)"
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
  "requestedAt": "datetime (ISO 8601)",
  "respondedAt": "datetime (ISO 8601, nullable)"
}
```

---

## Notes for UI Team

1. **CORS Configuration**: The API allows requests from `http://localhost:3000` and `http://localhost:5173` with credentials.

2. **Link Request Workflow**:
   - Parent requests link using student's email
   - Link is created with `PENDING` status
   - Student receives notification (implement notification system)
   - Student approves/rejects via StudentLinkController
   - Status changes to `ACTIVE` or `REJECTED`

3. **Parent Dashboard Views**:
   - **Active Students**: Use `/parents/{parentUserId}/students` or `/parents/{parentUserId}/links?status=ACTIVE`
   - **Pending Requests**: Use `/parents/{parentUserId}/links?status=PENDING`
   - **All Links**: Use `/parents/{parentUserId}/links` (no filter)

4. **Status Indicators**:
   - `PENDING`: Show "Waiting for approval" with clock icon
   - `ACTIVE`: Show "Connected" with checkmark icon
   - `REJECTED`: Show "Request declined" with X icon
   - `REVOKED`: Show "Access revoked" with warning icon

5. **Relationship Types**:
   - Display relationship type in parent-student list
   - Allow parent to specify relationship when requesting link
   - Use for filtering or grouping in UI

6. **Access Control**:
   - Only show student data if link status is `ACTIVE`
   - Hide or disable features for `PENDING` links
   - Show appropriate messages for `REJECTED` or `REVOKED` links

7. **Notifications**:
   - Notify student when parent requests link
   - Notify parent when student approves/rejects
   - Show badge count for pending requests

8. **Revoking Access**:
   - Use DELETE endpoint to revoke access
   - Show confirmation dialog before revoking
   - Both parent and student can revoke access

9. **Multiple Children**:
   - Parents can link to multiple students
   - Show all children in a list or card view
   - Allow switching between children's data

10. **Error Handling**:
    - Handle 404 when parent or student not found
    - Handle 409 when duplicate link request exists
    - Handle 400 for invalid email formats
    - Provide user-friendly error messages

11. **Student Approval Flow**:
    - Student sees pending requests in StudentLinkController
    - Student can approve or reject
    - See StudentLinkController specification for approval endpoints

12. **Security Considerations**:
    - Verify parent identity before showing student data
    - Log all link/unlink operations
    - Implement rate limiting for link requests
    - Validate email addresses before creating requests