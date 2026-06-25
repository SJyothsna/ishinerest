# Feedback Controller API Specification

## Overview
The Feedback Controller handles contact form submissions and user feedback. It provides endpoints for users to submit inquiries, feedback, support requests, and other messages. The system automatically sends email notifications to both administrators and users.

**Base URL:** `/feedback`

**Controller Class:** `FeedbackController.java`

---

## Endpoints

### 1. Submit Feedback

Submit a new contact form or feedback message.

**Endpoint:** `POST /feedback`

**Authentication:** Optional (can be used by both authenticated and anonymous users)

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "type": "feedback",
  "subject": "Great platform!",
  "message": "I love using iStudy for my studies. The practice questions are very helpful."
}
```

**Field Validations:**
- `name`: Required, string, 1-255 characters
- `email`: Required, valid email format, 1-255 characters
- `type`: Required, must be one of: `inquiry`, `feedback`, `support`, `other` (case-insensitive)
- `subject`: Required, string, 1-500 characters
- `message`: Required, string, minimum 10 characters

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "Thank you for your feedback! We'll get back to you soon.",
  "feedbackId": 123
}
```

**Error Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Validation error: Invalid email format",
  "feedbackId": null
}
```

**Error Response (500 Internal Server Error):**
```json
{
  "success": false,
  "message": "Failed to submit feedback. Please try again later.",
  "feedbackId": null
}
```

**Example cURL:**
```bash
curl -X POST http://localhost:8080/feedback \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "type": "feedback",
    "subject": "Great platform!",
    "message": "I love using iStudy for my studies. The practice questions are very helpful."
  }'
```

---

## Email Notifications

### 1. Admin Notification Email

Sent to: `support@istudy.ie` (configurable via `app.support.email`)

**Subject:** `New [type] from [name] - [subject]`

**Content Includes:**
- Feedback ID
- Type and Status
- Submission timestamp
- Contact information (name, email)
- Subject
- Full message content

### 2. User Confirmation Email

Sent to: User's email address

**Subject:** `We received your message - iStudy`

**Content Includes:**
- Personalized greeting
- Confirmation of receipt
- Message summary (subject, type, reference ID)
- Expected response time (24-48 hours)
- Contact information for follow-up

---

## Database Schema

### Table: `feedback`

```sql
CREATE TABLE feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL,
  type ENUM('INQUIRY', 'FEEDBACK', 'SUPPORT', 'OTHER') NOT NULL DEFAULT 'INQUIRY',
  subject VARCHAR(500) NOT NULL,
  message TEXT NOT NULL,
  status ENUM('NEW', 'READ', 'IN_PROGRESS', 'RESOLVED') NOT NULL DEFAULT 'NEW',
  user_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  INDEX idx_status (status),
  INDEX idx_type (type),
  INDEX idx_created_at (created_at),
  INDEX idx_email (email),
  
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
);
```

---

## Entity Classes

### Feedback Entity
- **Package:** `com.ishine.ishinerest.entity`
- **File:** `Feedback.java`
- **Key Fields:**
  - `id`: Long (Primary Key)
  - `name`: String
  - `email`: String
  - `type`: FeedbackType enum
  - `subject`: String
  - `message`: String
  - `status`: FeedbackStatus enum
  - `user`: User (optional relationship)
  - `createdAt`: LocalDateTime
  - `updatedAt`: LocalDateTime

### Enums

**FeedbackType:**
- `INQUIRY` - General inquiries
- `FEEDBACK` - User feedback
- `SUPPORT` - Technical support requests
- `OTHER` - Other types of messages

**FeedbackStatus:**
- `NEW` - Newly submitted, not yet reviewed
- `READ` - Reviewed by admin
- `IN_PROGRESS` - Being actively worked on
- `RESOLVED` - Issue resolved or inquiry answered

---

## Request/Response POJOs

### SubmitFeedbackRequest
- **Package:** `com.ishine.ishinerest.pojo`
- **File:** `SubmitFeedbackRequest.java`
- **Type:** Record
- **Fields:** name, email, type, subject, message
- **Validation:** Jakarta Bean Validation annotations

### FeedbackResponse
- **Package:** `com.ishine.ishinerest.pojo`
- **File:** `FeedbackResponse.java`
- **Type:** Record
- **Fields:** success, message, feedbackId
- **Factory Methods:**
  - `success(Long feedbackId)` - Creates success response
  - `error(String message)` - Creates error response

---

## Service Layer

### FeedbackService
- **Package:** `com.ishine.ishinerest.service`
- **File:** `FeedbackService.java`

**Key Methods:**
- `submitFeedback(SubmitFeedbackRequest, Long userId)` - Process feedback submission
- `sendAdminNotification(Feedback)` - Send email to admin (async)
- `sendUserConfirmation(Feedback)` - Send confirmation to user (async)

**Configuration Properties:**
- `spring.mail.username` - From email address (default: noreply@istudy.ie)
- `app.support.email` - Admin email address (default: support@istudy.ie)

---

## Repository Layer

### FeedbackRepository
- **Package:** `com.ishine.ishinerest.repository`
- **File:** `FeedbackRepository.java`
- **Extends:** `JpaRepository<Feedback, Long>`

**Query Methods:**
- `findByStatusOrderByCreatedAtDesc(FeedbackStatus)` - Find by status
- `findByTypeOrderByCreatedAtDesc(FeedbackType)` - Find by type
- `findByEmailOrderByCreatedAtDesc(String)` - Find by email
- `findByUser_UserIdOrderByCreatedAtDesc(Long)` - Find by user ID

---

## Error Handling

### Validation Errors (400)
- Invalid email format
- Missing required fields
- Field length violations
- Invalid type value

### Server Errors (500)
- Database connection issues
- Email sending failures (logged but don't block submission)
- Unexpected exceptions

**Note:** Email failures are logged but do not cause the API to return an error. The feedback is still saved to the database.

---

## Security Considerations

1. **Rate Limiting:** Consider implementing rate limiting to prevent spam
2. **CORS:** Configured for localhost:3000 and localhost:5173
3. **Input Validation:** All inputs are validated using Jakarta Bean Validation
4. **SQL Injection:** Protected by JPA/Hibernate parameterized queries
5. **XSS Prevention:** Frontend should sanitize display of user-submitted content

---

## Testing

### Test File Location
`src/test/java/rest/feedback/feedback.http`

### Test Scenarios
1. Valid feedback submission
2. Invalid email format
3. Missing required fields
4. Message too short
5. Invalid type value
6. Authenticated user submission

---

## Future Enhancements

1. **Admin Dashboard:** View and manage feedback submissions
2. **Status Updates:** Allow admins to update feedback status
3. **Response System:** Allow admins to respond directly through the system
4. **Analytics:** Track feedback trends and common issues
5. **File Attachments:** Allow users to attach screenshots or documents
6. **Priority Levels:** Add priority field for urgent issues
7. **Categories:** Add more granular categorization
8. **Auto-responses:** Implement automated responses based on keywords

---

## Related Documentation

- [Email Service Documentation](../service/EmailService.java)
- [Database Migration Script](../../../database_migration_create_feedback_table.sql)
- [User Controller API](./UserController-API-Specification.md)

---

**Last Updated:** 2024  
**Author:** Bob  
**Version:** 1.0