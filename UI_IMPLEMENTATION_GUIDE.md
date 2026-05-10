# Parent Feature - UI Implementation Guide

Complete API reference for frontend developers with all endpoints, request/response formats, and usage examples.

---

## Base URL
```
http://localhost:8080
```

---

## Authentication
All endpoints require authentication. Include JWT token in headers:
```javascript
headers: {
  'Authorization': 'Bearer YOUR_JWT_TOKEN',
  'Content-Type': 'application/json'
}
```

---

## API Endpoints Reference

### 1. Parent: Link Student by Email

**Use Case:** Parent wants to link a student using their email address

**Endpoint:**
```
POST /api/parents/{parentUserId}/link-student
```

**URL Parameters:**
- `parentUserId` (number) - The parent's user ID

**Request Body:**
```json
{
  "studentEmail": "student@example.com",
  "relationshipType": "MOTHER"
}
```

**Request Body Fields:**
- `studentEmail` (string, required) - Student's email address
- `relationshipType` (string, optional) - MOTHER, FATHER, GUARDIAN, etc.

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

404 Not Found:
```json
{
  "timestamp": "2026-05-08T19:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "No student account exists with this email address",
  "path": "/api/parents/1/link-student"
}
```

409 Conflict:
```json
{
  "timestamp": "2026-05-08T19:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "A pending link request already exists for this student",
  "path": "/api/parents/1/link-student"
}
```

**JavaScript Example:**
```javascript
async function linkStudentByEmail(parentUserId, studentEmail, relationshipType) {
  const response = await fetch(`http://localhost:8080/api/parents/${parentUserId}/link-student`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      studentEmail: studentEmail,
      relationshipType: relationshipType
    })
  });
  
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }
  
  return await response.json();
}

// Usage
try {
  const link = await linkStudentByEmail(1, 'student@example.com', 'MOTHER');
  console.log('Link request sent:', link);
  alert('Link request sent successfully! Waiting for student approval.');
} catch (error) {
  console.error('Error:', error.message);
  alert(error.message);
}
```

---

### 2. Parent: Get All Linked Students

**Use Case:** Parent wants to see all their actively linked students

**Endpoint:**
```
GET /api/parents/{parentUserId}/students
```

**URL Parameters:**
- `parentUserId` (number) - The parent's user ID

**Request Body:** None

**Success Response (200 OK):**
```json
[
  {
    "userId": 5,
    "name": "John Doe",
    "email": "student@example.com",
    "role": "STUDENT",
    "isActive": true,
    "createdAt": "2026-01-01T10:00:00",
    "updatedAt": "2026-05-08T19:30:00"
  },
  {
    "userId": 8,
    "name": "Jane Doe",
    "email": "jane.student@example.com",
    "role": "STUDENT",
    "isActive": true,
    "createdAt": "2026-02-15T14:20:00",
    "updatedAt": "2026-05-08T19:30:00"
  }
]
```

**JavaScript Example:**
```javascript
async function getLinkedStudents(parentUserId) {
  const response = await fetch(`http://localhost:8080/api/parents/${parentUserId}/students`, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  if (!response.ok) {
    throw new Error('Failed to fetch students');
  }
  
  return await response.json();
}

// Usage
const students = await getLinkedStudents(1);
console.log('Linked students:', students);
```

---

### 3. Parent: Get All Links with Status Filter

**Use Case:** Parent wants to see pending requests, rejected requests, or all links

**Endpoint:**
```
GET /api/parents/{parentUserId}/links?status={status}
```

**URL Parameters:**
- `parentUserId` (number) - The parent's user ID

**Query Parameters:**
- `status` (string, optional) - Filter by status: PENDING, ACTIVE, REJECTED, REVOKED
  - If omitted, returns all links

**Request Body:** None

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
  },
  {
    "parentUserId": 1,
    "parentName": "Jane Smith",
    "parentEmail": "jane@example.com",
    "studentUserId": 8,
    "studentName": "Jane Doe",
    "studentEmail": "jane.student@example.com",
    "relationshipType": "MOTHER",
    "status": "ACTIVE",
    "createdAt": "2026-02-15T14:20:00",
    "approvedAt": "2026-02-15T15:00:00",
    "rejectedAt": null
  }
]
```

**JavaScript Example:**
```javascript
async function getParentLinks(parentUserId, status = null) {
  let url = `http://localhost:8080/api/parents/${parentUserId}/links`;
  if (status) {
    url += `?status=${status}`;
  }
  
  const response = await fetch(url, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  if (!response.ok) {
    throw new Error('Failed to fetch links');
  }
  
  return await response.json();
}

// Usage examples
const allLinks = await getParentLinks(1);
const pendingLinks = await getParentLinks(1, 'PENDING');
const activeLinks = await getParentLinks(1, 'ACTIVE');
const rejectedLinks = await getParentLinks(1, 'REJECTED');
```

---

### 4. Parent: Unlink Student (Revoke Access)

**Use Case:** Parent wants to remove a student link

**Endpoint:**
```
DELETE /api/parents/{parentUserId}/students/{studentUserId}
```

**URL Parameters:**
- `parentUserId` (number) - The parent's user ID
- `studentUserId` (number) - The student's user ID

**Request Body:** None

**Success Response:** `204 No Content` (empty body)

**Error Response (404 Not Found):**
```json
{
  "timestamp": "2026-05-08T19:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Parent-student relationship not found",
  "path": "/api/parents/1/students/5"
}
```

**JavaScript Example:**
```javascript
async function unlinkStudent(parentUserId, studentUserId) {
  const response = await fetch(
    `http://localhost:8080/api/parents/${parentUserId}/students/${studentUserId}`,
    {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }
  
  return true; // Success
}

// Usage
if (confirm('Are you sure you want to unlink this student?')) {
  try {
    await unlinkStudent(1, 5);
    alert('Student unlinked successfully');
    // Refresh the student list
  } catch (error) {
    alert('Error: ' + error.message);
  }
}
```

---

### 5. Student: Get Pending Link Requests

**Use Case:** Student wants to see all pending parent link requests

**Endpoint:**
```
GET /api/students/{studentUserId}/link-requests
```

**URL Parameters:**
- `studentUserId` (number) - The student's user ID

**Request Body:** None

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
  },
  {
    "parentUserId": 3,
    "parentName": "Bob Johnson",
    "parentEmail": "bob@example.com",
    "studentUserId": 5,
    "studentName": "John Doe",
    "studentEmail": "student@example.com",
    "relationshipType": "FATHER",
    "status": "PENDING",
    "createdAt": "2026-05-08T18:00:00",
    "approvedAt": null,
    "rejectedAt": null
  }
]
```

**JavaScript Example:**
```javascript
async function getPendingLinkRequests(studentUserId) {
  const response = await fetch(
    `http://localhost:8080/api/students/${studentUserId}/link-requests`,
    {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  
  if (!response.ok) {
    throw new Error('Failed to fetch link requests');
  }
  
  return await response.json();
}

// Usage
const requests = await getPendingLinkRequests(5);
console.log(`You have ${requests.length} pending link requests`);
```

---

### 6. Student: Approve Link Request

**Use Case:** Student approves a parent's link request

**Endpoint:**
```
POST /api/students/link-requests/{parentUserId}/approve
```

**URL Parameters:**
- `parentUserId` (number) - The parent's user ID who sent the request

**Request Body:**
```json
{
  "studentUserId": 5
}
```

**Request Body Fields:**
- `studentUserId` (number, required) - The student's user ID

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

404 Not Found:
```json
{
  "timestamp": "2026-05-08T19:35:00",
  "status": 404,
  "error": "Not Found",
  "message": "Link request not found",
  "path": "/api/students/link-requests/1/approve"
}
```

400 Bad Request:
```json
{
  "timestamp": "2026-05-08T19:35:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Link request has already been processed",
  "path": "/api/students/link-requests/1/approve"
}
```

**JavaScript Example:**
```javascript
async function approveLinkRequest(parentUserId, studentUserId) {
  const response = await fetch(
    `http://localhost:8080/api/students/link-requests/${parentUserId}/approve`,
    {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        studentUserId: studentUserId
      })
    }
  );
  
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }
  
  return await response.json();
}

// Usage
try {
  const link = await approveLinkRequest(1, 5);
  alert('Parent link approved successfully!');
  console.log('Approved link:', link);
  // Refresh the requests list
} catch (error) {
  alert('Error: ' + error.message);
}
```

---

### 7. Student: Reject Link Request

**Use Case:** Student rejects a parent's link request

**Endpoint:**
```
POST /api/students/link-requests/{parentUserId}/reject
```

**URL Parameters:**
- `parentUserId` (number) - The parent's user ID who sent the request

**Request Body:**
```json
{
  "studentUserId": 5
}
```

**Request Body Fields:**
- `studentUserId` (number, required) - The student's user ID

**Success Response:** `204 No Content` (empty body)

**Error Responses:**

404 Not Found:
```json
{
  "timestamp": "2026-05-08T19:35:00",
  "status": 404,
  "error": "Not Found",
  "message": "Link request not found",
  "path": "/api/students/link-requests/1/reject"
}
```

400 Bad Request:
```json
{
  "timestamp": "2026-05-08T19:35:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Link request has already been processed",
  "path": "/api/students/link-requests/1/reject"
}
```

**JavaScript Example:**
```javascript
async function rejectLinkRequest(parentUserId, studentUserId) {
  const response = await fetch(
    `http://localhost:8080/api/students/link-requests/${parentUserId}/reject`,
    {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        studentUserId: studentUserId
      })
    }
  );
  
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }
  
  return true; // Success
}

// Usage
if (confirm('Are you sure you want to reject this link request?')) {
  try {
    await rejectLinkRequest(1, 5);
    alert('Link request rejected');
    // Refresh the requests list
  } catch (error) {
    alert('Error: ' + error.message);
  }
}
```

---

### 8. Check if Parent is Linked to Student

**Use Case:** Check if a specific parent-student link is active

**Endpoint:**
```
GET /api/parents/{parentUserId}/students/{studentUserId}/linked
```

**URL Parameters:**
- `parentUserId` (number) - The parent's user ID
- `studentUserId` (number) - The student's user ID

**Request Body:** None

**Success Response (200 OK):**
```json
true
```
or
```json
false
```

**JavaScript Example:**
```javascript
async function isParentLinked(parentUserId, studentUserId) {
  const response = await fetch(
    `http://localhost:8080/api/parents/${parentUserId}/students/${studentUserId}/linked`,
    {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  
  if (!response.ok) {
    throw new Error('Failed to check link status');
  }
  
  return await response.json();
}

// Usage
const isLinked = await isParentLinked(1, 5);
if (isLinked) {
  console.log('Parent is linked to student');
} else {
  console.log('Parent is not linked to student');
}
```

---

## Complete React/JavaScript Service Example

```javascript
// parentLinkService.js
const API_BASE = 'http://localhost:8080';

class ParentLinkService {
  constructor(token) {
    this.token = token;
  }

  async linkStudentByEmail(parentUserId, studentEmail, relationshipType = null) {
    const response = await fetch(`${API_BASE}/api/parents/${parentUserId}/link-student`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ studentEmail, relationshipType })
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message);
    }
    
    return await response.json();
  }

  async getLinkedStudents(parentUserId) {
    const response = await fetch(`${API_BASE}/api/parents/${parentUserId}/students`, {
      method: 'GET',
      headers: { 'Authorization': `Bearer ${this.token}` }
    });
    
    if (!response.ok) throw new Error('Failed to fetch students');
    return await response.json();
  }

  async getParentLinks(parentUserId, status = null) {
    let url = `${API_BASE}/api/parents/${parentUserId}/links`;
    if (status) url += `?status=${status}`;
    
    const response = await fetch(url, {
      method: 'GET',
      headers: { 'Authorization': `Bearer ${this.token}` }
    });
    
    if (!response.ok) throw new Error('Failed to fetch links');
    return await response.json();
  }

  async unlinkStudent(parentUserId, studentUserId) {
    const response = await fetch(
      `${API_BASE}/api/parents/${parentUserId}/students/${studentUserId}`,
      {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${this.token}` }
      }
    );
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message);
    }
  }

  async getPendingLinkRequests(studentUserId) {
    const response = await fetch(
      `${API_BASE}/api/students/${studentUserId}/link-requests`,
      {
        method: 'GET',
        headers: { 'Authorization': `Bearer ${this.token}` }
      }
    );
    
    if (!response.ok) throw new Error('Failed to fetch requests');
    return await response.json();
  }

  async approveLinkRequest(parentUserId, studentUserId) {
    const response = await fetch(
      `${API_BASE}/api/students/link-requests/${parentUserId}/approve`,
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ studentUserId })
      }
    );
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message);
    }
    
    return await response.json();
  }

  async rejectLinkRequest(parentUserId, studentUserId) {
    const response = await fetch(
      `${API_BASE}/api/students/link-requests/${parentUserId}/reject`,
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ studentUserId })
      }
    );
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message);
    }
  }

  async isParentLinked(parentUserId, studentUserId) {
    const response = await fetch(
      `${API_BASE}/api/parents/${parentUserId}/students/${studentUserId}/linked`,
      {
        method: 'GET',
        headers: { 'Authorization': `Bearer ${this.token}` }
      }
    );
    
    if (!response.ok) throw new Error('Failed to check link status');
    return await response.json();
  }
}

export default ParentLinkService;
```

---

## Usage Examples in React Components

### Parent Dashboard Component

```jsx
import React, { useState, useEffect } from 'react';
import ParentLinkService from './services/parentLinkService';

function ParentDashboard({ parentUserId, token }) {
  const [students, setStudents] = useState([]);
  const [pendingLinks, setPendingLinks] = useState([]);
  const [studentEmail, setStudentEmail] = useState('');
  const [relationshipType, setRelationshipType] = useState('MOTHER');
  const service = new ParentLinkService(token);

  useEffect(() => {
    loadStudents();
    loadPendingLinks();
  }, []);

  const loadStudents = async () => {
    try {
      const data = await service.getLinkedStudents(parentUserId);
      setStudents(data);
    } catch (error) {
      console.error('Error loading students:', error);
    }
  };

  const loadPendingLinks = async () => {
    try {
      const data = await service.getParentLinks(parentUserId, 'PENDING');
      setPendingLinks(data);
    } catch (error) {
      console.error('Error loading pending links:', error);
    }
  };

  const handleLinkStudent = async (e) => {
    e.preventDefault();
    try {
      await service.linkStudentByEmail(parentUserId, studentEmail, relationshipType);
      alert('Link request sent! Waiting for student approval.');
      setStudentEmail('');
      loadPendingLinks();
    } catch (error) {
      alert('Error: ' + error.message);
    }
  };

  const handleUnlink = async (studentUserId) => {
    if (!confirm('Are you sure you want to unlink this student?')) return;
    
    try {
      await service.unlinkStudent(parentUserId, studentUserId);
      alert('Student unlinked successfully');
      loadStudents();
    } catch (error) {
      alert('Error: ' + error.message);
    }
  };

  return (
    <div>
      <h2>Parent Dashboard</h2>
      
      {/* Link Student Form */}
      <form onSubmit={handleLinkStudent}>
        <h3>Link a Student</h3>
        <input
          type="email"
          placeholder="Student Email"
          value={studentEmail}
          onChange={(e) => setStudentEmail(e.target.value)}
          required
        />
        <select 
          value={relationshipType}
          onChange={(e) => setRelationshipType(e.target.value)}
        >
          <option value="MOTHER">Mother</option>
          <option value="FATHER">Father</option>
          <option value="GUARDIAN">Guardian</option>
        </select>
        <button type="submit">Send Link Request</button>
      </form>

      {/* Pending Requests */}
      {pendingLinks.length > 0 && (
        <div>
          <h3>Pending Requests ({pendingLinks.length})</h3>
          <ul>
            {pendingLinks.map(link => (
              <li key={link.studentUserId}>
                {link.studentName} ({link.studentEmail}) - Waiting for approval
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Linked Students */}
      <div>
        <h3>My Students ({students.length})</h3>
        <ul>
          {students.map(student => (
            <li key={student.userId}>
              {student.name} ({student.email})
              <button onClick={() => handleUnlink(student.userId)}>
                Unlink
              </button>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

export default ParentDashboard;
```

### Student Link Requests Component

```jsx
import React, { useState, useEffect } from 'react';
import ParentLinkService from './services/parentLinkService';

function StudentLinkRequests({ studentUserId, token }) {
  const [requests, setRequests] = useState([]);
  const service = new ParentLinkService(token);

  useEffect(() => {
    loadRequests();
  }, []);

  const loadRequests = async () => {
    try {
      const data = await service.getPendingLinkRequests(studentUserId);
      setRequests(data);
    } catch (error) {
      console.error('Error loading requests:', error);
    }
  };

  const handleApprove = async (parentUserId) => {
    try {
      await service.approveLinkRequest(parentUserId, studentUserId);
      alert('Parent link approved!');
      loadRequests();
    } catch (error) {
      alert('Error: ' + error.message);
    }
  };

  const handleReject = async (parentUserId) => {
    if (!confirm('Are you sure you want to reject this request?')) return;
    
    try {
      await service.rejectLinkRequest(parentUserId, studentUserId);
      alert('Request rejected');
      loadRequests();
    } catch (error) {
      alert('Error: ' + error.message);
    }
  };

  return (
    <div>
      <h2>Parent Link Requests</h2>
      {requests.length === 0 ? (
        <p>No pending requests</p>
      ) : (
        <ul>
          {requests.map(request => (
            <li key={request.parentUserId}>
              <div>
                <strong>{request.parentName}</strong> ({request.parentEmail})
                <br />
                Relationship: {request.relationshipType || 'Not specified'}
                <br />
                Requested: {new Date(request.createdAt).toLocaleDateString()}
              </div>
              <button onClick={() => handleApprove(request.parentUserId)}>
                Approve
              </button>
              <button onClick={() => handleReject(request.parentUserId)}>
                Reject
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default StudentLinkRequests;
```

---

## Status Values Reference

```javascript
const LinkStatus = {
  PENDING: 'PENDING',   // Waiting for student approval
  ACTIVE: 'ACTIVE',     // Approved and active
  REJECTED: 'REJECTED', // Student rejected the request
  REVOKED: 'REVOKED'    // Link was removed by parent or student
};
```

---

## Error Handling Best Practices

```javascript
async function handleApiCall(apiFunction) {
  try {
    const result = await apiFunction();
    return { success: true, data: result };
  } catch (error) {
    console.error('API Error:', error);
    
    // Handle specific error types
    if (error.message.includes('not found')) {
      return { success: false, error: 'Resource not found' };
    } else if (error.message.includes('already exists')) {
      return { success: false, error: 'Link already exists' };
    } else if (error.message.includes('already been processed')) {
      return { success: false, error: 'Request already processed' };
    }
    
    return { success: false, error: error.message };
  }
}

// Usage
const result = await handleApiCall(() => 
  service.linkStudentByEmail(1, 'student@example.com', 'MOTHER')
);

if (result.success) {
  console.log('Success:', result.data);
} else {
  console.error('Error:', result.error);
  alert(result.error);
}
```

---

## Testing Checklist for UI Developers

- [ ] Parent can send link request with valid email
- [ ] Error shown when email doesn't exist
- [ ] Error shown when link already exists
- [ ] Student can view pending requests
- [ ] Student can approve request
- [ ] Student can reject request
- [ ] Parent sees pending requests count
- [ ] Parent sees only active students in main list
- [ ] Parent can unlink student
- [ ] Proper error messages displayed
- [ ] Loading states handled
- [ ] Success messages shown
- [ ] Lists refresh after actions

---

*UI Implementation Guide - Generated: 2026-05-08*
*Made with Bob*