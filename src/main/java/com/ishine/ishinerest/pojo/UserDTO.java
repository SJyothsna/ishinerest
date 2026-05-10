package com.ishine.ishinerest.pojo;

import com.ishine.ishinerest.entity.User;
import com.ishine.ishinerest.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for User entity - used in API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long userId;
    private String name;
    private String email;
    private UserRole role;
    private Boolean isActive;
    private LocalDateTime deletedAt;
    
    /**
     * Convert User entity to DTO
     */
    public static UserDTO fromEntity(User user) {
        return new UserDTO(
            user.getUserId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.getIsActive(),
            user.getDeletedAt()
        );
    }
}

// Made with Bob