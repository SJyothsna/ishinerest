package com.ishine.ishinerest.pojo;

/**
 * Response DTO for teacher add-or-link student flow.
 */
public record AddStudentByEmailResponse(
    boolean createdNewStudent,
    boolean linked,
    UserDTO student,
    String temporaryPassword
) {}

// Made with Bob