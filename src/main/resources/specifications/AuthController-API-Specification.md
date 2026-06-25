# Authentication API Specification

## Base URL
`/auth`

## Overview
Handles user authentication, registration, password reset, and email verification.

---

## Endpoints

### 1. User Signup
**POST** `/auth/signup`

Creates a new user account.

#### Request Body
```json
{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "password": "SecurePass123!",
  "role": "STUDENT"
}
```

#### Response (201 Created)
```json
{
  "userId": 1,
  "name": "John Doe",
  "email": "john.doe@example.com",
  "role": "STUDENT",
  "message": "User registered successfully. Please check your email to verify your account."
}
```

#### Error Responses
- **400 Bad Request**: Invalid input data
- **409 Conflict**: Email already in use

---

### 2. User Login
**POST** `/auth/login`

Authenticates a user and returns login credentials.

#### Request Body
```json
{
  "email": "john.doe@example.com",
  "password": "SecurePass123!"
}
```

#### Response (200 OK)
```json
{
  "userId": 1,
  "name": "John Doe",
  "email": "john.doe@example.com",
  "role": "STUDENT",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "message": "Login successful"
}
```

#### Error Responses
- **401 Unauthorized**: Invalid credentials
- **403 Forbidden**: Account not verified or deactivated

---

### 3. Forgot Password
**POST** `/auth/forgot-password`

Initiates password reset process by sending a reset token via email.

#### Request Body
```json
{
  "email": "john.doe@example.com"
}
```

#### Response (200 OK)
```json
{
  "success": true,
  "message": "Password reset email sent successfully"
}
```

#### Error Responses
- **404 Not Found**: Email not found

---

### 4. Validate Reset Token
**GET** `/auth/validate-reset-token?token={token}`

Validates if a password reset token is valid and not expired.

#### Query Parameters
- `token` (required): The reset token received via email

#### Response (200 OK)
```json
{
  "valid": true,
  "message": "Token is valid"
}
```

#### Error Responses
- **400 Bad Request**: Invalid or expired token

---

### 5. Reset Password
**POST** `/auth/reset-password`

Resets user password using a valid reset token.

#### Request Body
```json
{
  "token": "abc123def456",
  "newPassword": "NewSecurePass123!"
}
```

#### Response (200 OK)
```json
{
  "success": true,
  "message": "Password reset successfully"
}
```

#### Error Responses
- **400 Bad Request**: Invalid token or password requirements not met
- **404 Not Found**: Token not found or expired

---

### 6. Verify Email
**GET** `/auth/verify-email?token={token}`

Verifies user email address using the verification token.

#### Query Parameters
- `token` (required): The verification token received via email

#### Response (200 OK)
```json
{
  "success": true,
  "message": "Email verified successfully"
}
```

#### Error Responses
- **400 Bad Request**: Invalid or expired token
- **404 Not Found**: Token not found

---

### 7. Resend Verification Email
**POST** `/auth/resend-verification?email={email}`

Resends the email verification link to the user.

#### Query Parameters
- `email` (required): User's email address

#### Response (200 OK)
```json
{
  "success": true,
  "message": "Verification email sent successfully"
}
```

#### Error Responses
- **404 Not Found**: Email not found
- **400 Bad Request**: Email already verified

---

## Data Models

### SignupRequest
```json
{
  "name": "string (required, 2-100 characters)",
  "email": "string (required, valid email format)",
  "password": "string (required, min 8 characters)",
  "role": "enum (STUDENT, TEACHER, PARENT, ADMIN)"
}
```

### LoginRequest
```json
{
  "email": "string (required, valid email format)",
  "password": "string (required)"
}
```

### ForgotPasswordRequest
```json
{
  "email": "string (required, valid email format)"
}
```

### ResetPasswordRequest
```json
{
  "token": "string (required)",
  "newPassword": "string (required, min 8 characters)"
}
```

---

## Notes for UI Team

1. **CORS Configuration**: The API allows requests from `http://localhost:3000` and `http://localhost:5173` with credentials.

2. **Password Requirements**: 
   - Minimum 8 characters
   - Should include uppercase, lowercase, numbers, and special characters

3. **Email Verification**: 
   - Users must verify their email before they can fully access the system
   - Verification tokens expire after a certain period (check with backend team)

4. **Password Reset Flow**:
   - User requests password reset → receives email with token
   - User clicks link with token → validates token
   - User enters new password → password is reset

5. **Token Storage**: Store authentication tokens securely (e.g., httpOnly cookies or secure storage)

6. **Error Handling**: All endpoints return appropriate HTTP status codes with error messages in the response body