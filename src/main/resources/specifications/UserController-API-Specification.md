# User API Specification

## Base URL
`/users`

## Overview
Manages user accounts, profiles, passwords, and account status across all user roles (STUDENT, TEACHER, PARENT, ADMIN).

---

## Endpoints

### 1. Get User by ID
**GET** `/users/{userId}`

Retrieves a user by their ID.

#### Path Parameters
- `userId` (required): The user ID

#### Response (200 OK)
```json
{
  "userId": 1,
  "name": "John Doe",
  "email": "john.doe@example.com",
  "role": "STUDENT",
  "isActive": true,
  "emailVerified": true,
  "createdAt": "2024-01-15T10:30:00Z"
}
```

---

### 2. Get User by Email
**GET** `/users/email/{email}`

Retrieves a user by their email address.

#### Path Parameters
- `email` (required): The user's email address

#### Response (200 OK)
```json
{
  "userId": 1,
  "name": "John Doe",
  "email": "john.doe@example.com",
  "role": "STUDENT",
  "isActive": true,
  "emailVerified": true,
  "createdAt": "2024-01-15T10:30:00Z"
}
```

---

### 3. Get All Active Users
**GET** `/users/active`

Retrieves all active users across all roles.

#### Response (200 OK)
```json
[
  {
    "userId": 1,
    "name": "John Doe",
    "email": "john.doe@example.com",
    "role": "STUDENT",
    "isActive": true,
    "emailVerified": true,
    "createdAt": "2024-01-15T10:30:00Z"
  },
  {
    "userId": 2,
    "name": "Jane Smith",
    "email": "jane.smith@example.com",
    "role": "TEACHER",
    "isActive": true,
    "emailVerified": true,
    "createdAt": "2024-01-16T09:15:00Z"
  }
]
```

---

### 4. Get Users by Role
**GET** `/users/role/{role}`

Retrieves all users with a specific role (active and inactive).

#### Path Parameters
- `role` (required): User role (STUDENT, TEACHER, PARENT, ADMIN)

#### Response (200 OK)
```json
[
  {
    "userId": 1,
    "name": "John Doe",
    "email": "john.doe@example.com",
    "role": "STUDENT",
    "isActive": true,
    "emailVerified": true,
    "createdAt": "2024-01-15T10:30:00Z"
  },
  {
    "userId": 3,
    "name": "Bob Wilson",
    "email": "bob.wilson@example.com",
    "role": "STUDENT",
    "isActive": false,
    "emailVerified": true,
    "createdAt": "2024-01-10T14:20:00Z"
  }
]
```

---

### 5. Get Active Users by Role
**GET** `/users/role/{role}/active`

Retrieves only active users with a specific role.

#### Path Parameters
- `role` (required): User role (STUDENT, TEACHER, PARENT, ADMIN)

#### Response (200 OK)
```json
[
  {
    "userId": 1,
    "name": "John Doe",
    "email": "john.doe@example.com",
    "role": "STUDENT",
    "isActive": true,
    "emailVerified": true,
    "createdAt": "2024-01-15T10:30:00Z"
  }
]
```

---

### 6. Update User Profile
**PUT** `/users/{userId}/profile`

Updates user's name and email.

#### Path Parameters
- `userId` (required): The user ID

#### Request Body
```json
{
  "name": "John Michael Doe",
  "email": "john.m.doe@example.com"
}
```

#### Response (200 OK)
```json
{
  "userId": 1,
  "name": "John Michael Doe",
  "email": "john.m.doe@example.com",
  "role": "STUDENT",
  "isActive": true,
  "emailVerified": false,
  "createdAt": "2024-01-15T10:30:00Z"
}
```

#### Notes
- If email is changed, `emailVerified` will be set to `false` and a new verification email will be sent

---

### 7. Change Password
**PUT** `/users/{userId}/password`

Changes user's password.

#### Path Parameters
- `userId` (required): The user ID

#### Request Body
```json
{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewSecurePass456!"
}
```

#### Response (204 No Content)
```
(Empty response body)
```

#### Error Responses
- **400 Bad Request**: Current password is incorrect or new password doesn't meet requirements
- **401 Unauthorized**: User not authenticated

---

### 8. Deactivate User
**DELETE** `/users/{userId}`

Soft deletes a user account (sets isActive to false).

#### Path Parameters
- `userId` (required): The user ID

#### Response (204 No Content)
```
(Empty response body)
```

---

### 9. Deactivate User (Alternative)
**PUT** `/users/{userId}/deactivate`

Alternative endpoint for deactivating a user (for admin UI compatibility).

#### Path Parameters
- `userId` (required): The user ID

#### Response (204 No Content)
```
(Empty response body)
```

---

### 10. Reactivate User
**POST** `/users/{userId}/reactivate`

Reactivates a deactivated user account.

#### Path Parameters
- `userId` (required): The user ID

#### Response (204 No Content)
```
(Empty response body)
```

---

### 11. Activate User (Alternative)
**PUT** `/users/{userId}/activate`

Alternative endpoint for activating a user (for admin UI compatibility).

#### Path Parameters
- `userId` (required): The user ID

#### Response (204 No Content)
```
(Empty response body)
```

---

### 12. Hard Delete User
**DELETE** `/users/{userId}/hard-delete`

Permanently removes a user from the database. Only works for inactive users.

#### Path Parameters
- `userId` (required): The user ID

#### Response (204 No Content)
```
(Empty response body)
```

#### Error Responses
- **400 Bad Request**: Cannot hard delete an active user
- **404 Not Found**: User not found

---

### 13. Get All Users (Admin)
**GET** `/users`

Retrieves all users. Optionally filter by role.

#### Query Parameters
- `role` (optional): Filter by user role (STUDENT, TEACHER, PARENT, ADMIN)

#### Response (200 OK)
```json
[
  {
    "userId": 1,
    "name": "John Doe",
    "email": "john.doe@example.com",
    "role": "STUDENT",
    "isActive": true,
    "emailVerified": true,
    "createdAt": "2024-01-15T10:30:00Z"
  },
  {
    "userId": 2,
    "name": "Jane Smith",
    "email": "jane.smith@example.com",
    "role": "TEACHER",
    "isActive": true,
    "emailVerified": true,
    "createdAt": "2024-01-16T09:15:00Z"
  }
]
```

#### Example Usage
- Get all users: `GET /users`
- Get all students: `GET /users?role=STUDENT`
- Get all teachers: `GET /users?role=TEACHER`

---

## Data Models

### UserDTO
```json
{
  "userId": "long (unique identifier)",
  "name": "string (required, 2-100 characters)",
  "email": "string (required, valid email format)",
  "role": "enum (STUDENT, TEACHER, PARENT, ADMIN)",
  "isActive": "boolean",
  "emailVerified": "boolean",
  "createdAt": "datetime (ISO 8601 format)"
}
```

### UpdateUserProfileRequest
```json
{
  "name": "string (required, 2-100 characters)",
  "email": "string (required, valid email format)"
}
```

### ChangePasswordRequest
```json
{
  "currentPassword": "string (required)",
  "newPassword": "string (required, min 8 characters)"
}
```

---

## Notes for UI Team

1. **CORS Configuration**: The API allows requests from `http://localhost:3000` and `http://localhost:5173` with credentials.

2. **User Roles**: 
   - `STUDENT`: Regular student users
   - `TEACHER`: Teacher/instructor users
   - `PARENT`: Parent/guardian users
   - `ADMIN`: System administrators

3. **Account Status**:
   - `isActive`: Whether the account is active or deactivated
   - `emailVerified`: Whether the user has verified their email address

4. **Deactivation vs Hard Delete**:
   - Deactivation: Soft delete, user data is preserved but account is disabled
   - Hard Delete: Permanent removal from database, only allowed for inactive users
   - Use deactivation for normal account closure
   - Use hard delete only for data cleanup or GDPR compliance

5. **Email Change Flow**:
   - When email is changed, `emailVerified` becomes `false`
   - User must verify the new email address
   - Consider showing a verification pending message in the UI

6. **Password Requirements**:
   - Minimum 8 characters
   - Should include uppercase, lowercase, numbers, and special characters
   - Validate on client-side before submission

7. **Admin Features**:
   - Use `/users` endpoint for admin dashboards
   - Filter by role to show specific user types
   - Show both active and inactive users with visual indicators

8. **User Search**:
   - Use `/users/email/{email}` for email-based search
   - Use `/users/{userId}` for direct user lookup
   - Consider implementing client-side filtering for large user lists

9. **Error Handling**:
   - Handle 404 errors when user is not found
   - Handle 400 errors for validation failures
   - Handle 401 errors for authentication issues
   - Provide user-friendly error messages

10. **Security Considerations**:
    - Only allow users to update their own profile (unless admin)
    - Require current password for password changes
    - Show confirmation dialogs for deactivation and deletion
    - Log out user after password change