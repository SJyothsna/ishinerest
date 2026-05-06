// LoginResponse.java
package com.ishine.ishinerest.auth.dto;

import com.ishine.ishinerest.entity.UserRole;

public record LoginResponse(
        Long userId,
        String name,
        String email,
        UserRole role,
        Long studentId  // Only populated if role is STUDENT
) {}
