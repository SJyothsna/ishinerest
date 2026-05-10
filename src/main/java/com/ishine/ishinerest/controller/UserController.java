package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.entity.UserRole;
import com.ishine.ishinerest.pojo.ChangePasswordRequest;
import com.ishine.ishinerest.pojo.UpdateUserProfileRequest;
import com.ishine.ishinerest.pojo.UserDTO;
import com.ishine.ishinerest.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for User management
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}, allowCredentials = "true")
public class UserController {
    
    private final UserService userService;
    
    /**
     * Get user by ID
     * GET /users/{userId}
     */
    @GetMapping("/{userId}")
    public UserDTO getUserById(@PathVariable Long userId) {
        return UserDTO.fromEntity(userService.getUserById(userId));
    }
    
    /**
     * Get user by email
     * GET /users/email/{email}
     */
    @GetMapping("/email/{email}")
    public UserDTO getUserByEmail(@PathVariable String email) {
        return UserDTO.fromEntity(userService.getUserByEmail(email));
    }
    
    /**
     * Get all active users
     * GET /users/active
     */
    @GetMapping("/active")
    public List<UserDTO> getAllActiveUsers() {
        return userService.getAllActiveUsers().stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Get users by role
     * GET /users/role/{role}
     */
    @GetMapping("/role/{role}")
    public List<UserDTO> getUsersByRole(@PathVariable UserRole role) {
        return userService.getUsersByRole(role).stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Get active users by role
     * GET /users/role/{role}/active
     */
    @GetMapping("/role/{role}/active")
    public List<UserDTO> getActiveUsersByRole(@PathVariable UserRole role) {
        return userService.getActiveUsersByRole(role).stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Update user profile
     * PUT /users/{userId}/profile
     */
    @PutMapping("/{userId}/profile")
    public UserDTO updateUserProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        var user = userService.updateUserProfile(userId, request.name(), request.email());
        return UserDTO.fromEntity(user);
    }
    
    /**
     * Change user password
     * PUT /users/{userId}/password
     */
    @PutMapping("/{userId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @PathVariable Long userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request.currentPassword(), request.newPassword());
    }
    
    /**
     * Deactivate user account
     * DELETE /users/{userId}
     */
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateUser(@PathVariable Long userId) {
        userService.deactivateUser(userId);
    }
    
    /**
     * Deactivate user account (alternative endpoint for admin UI)
     * PUT /users/{userId}/deactivate
     */
    @PutMapping("/{userId}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateUserAlt(@PathVariable Long userId) {
        userService.deactivateUser(userId);
    }
    
    /**
     * Reactivate user account
     * POST /users/{userId}/reactivate
     */
    @PostMapping("/{userId}/reactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactivateUser(@PathVariable Long userId) {
        userService.reactivateUser(userId);
    }
    
    /**
     * Activate user account (alternative endpoint for admin UI)
     * PUT /users/{userId}/activate
     */
    @PutMapping("/{userId}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activateUser(@PathVariable Long userId) {
        userService.reactivateUser(userId);
    }
    
    /**
     * Hard delete user (permanently remove from database)
     * DELETE /users/{userId}/hard-delete
     * Only works for inactive users
     */
    @DeleteMapping("/{userId}/hard-delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hardDeleteUser(@PathVariable Long userId) {
        userService.hardDeleteUser(userId);
    }
    
    /**
     * Get all users (admin only)
     * GET /users
     */
    @GetMapping
    public List<UserDTO> getAllUsers() {
        return userService.getAllUsers().stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }
}

// Made with Bob
