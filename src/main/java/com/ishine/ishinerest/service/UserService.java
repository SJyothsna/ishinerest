package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.User;
import com.ishine.ishinerest.entity.UserRole;
import com.ishine.ishinerest.repository.ParentStudentRepository;
import com.ishine.ishinerest.repository.StudentRepository;
import com.ishine.ishinerest.repository.TeacherStudentRepository;
import com.ishine.ishinerest.repository.TeacherTestRepository;
import com.ishine.ishinerest.repository.TestAssignmentRepository;
import com.ishine.ishinerest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing User entities
 */
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentRepository studentRepository;
    private final ParentStudentRepository parentStudentRepository;
    private final TeacherStudentRepository teacherStudentRepository;
    private final TeacherTestRepository teacherTestRepository;
    private final TestAssignmentRepository testAssignmentRepository;
    
    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
    
    /**
     * Get user by email
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
    
    /**
     * Get all active users
     */
    @Transactional(readOnly = true)
    public List<User> getAllActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }
    
    /**
     * Get all users by role
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role);
    }
    
    /**
     * Get all active users by role
     */
    @Transactional(readOnly = true)
    public List<User> getActiveUsersByRole(UserRole role) {
        return userRepository.findByRoleAndIsActiveTrue(role);
    }
    
    /**
     * Update user profile (name, email)
     */
    @Transactional
    public User updateUserProfile(Long userId, String name, String email) {
        var user = getUserById(userId);
        
        // Check if email is being changed and if new email is already in use
        if (!user.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }
        
        user.setName(name);
        user.setEmail(email);
        return userRepository.save(user);
    }
    
    /**
     * Change user password
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        var user = getUserById(userId);
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }
        
        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    
    /**
     * Deactivate user account (soft delete)
     */
    @Transactional
    public void deactivateUser(Long userId) {
        var user = getUserById(userId);
        user.setIsActive(false);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }
    
    /**
     * Reactivate user account
     */
    @Transactional
    public void reactivateUser(Long userId) {
        var user = getUserById(userId);
        user.setIsActive(true);
        user.setDeletedAt(null);
        userRepository.save(user);
    }
    
    /**
     * Check if user exists by email
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    /**
     * Get all users (for admin)
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    /**
     * Hard delete user (permanently remove from database)
     * Should only be used for inactive users
     * Cascades to delete all related records
     */
    @Transactional
    public void hardDeleteUser(Long userId) {
        var user = getUserById(userId);
        
        // Safety check: only allow hard delete of inactive users
        if (user.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Cannot hard delete an active user. Please deactivate the user first.");
        }
        
        // Delete all related records first to avoid foreign key constraint violations
        
        // 1. Delete test assignments where user is the student
        var studentAssignments = testAssignmentRepository.findByStudent_UserIdOrderByAssignedAtDesc(userId);
        if (!studentAssignments.isEmpty()) {
            testAssignmentRepository.deleteAll(studentAssignments);
        }
        
        // 2. Delete test assignments where user assigned the test
        var assignedByAssignments = testAssignmentRepository.findByAssignedBy_UserIdOrderByAssignedAtDesc(userId);
        if (!assignedByAssignments.isEmpty()) {
            testAssignmentRepository.deleteAll(assignedByAssignments);
        }
        
        // 3. Delete teacher tests created by the user
        var teacherTests = teacherTestRepository.findByCreatedBy_UserIdOrderByCreatedAtDesc(userId);
        if (!teacherTests.isEmpty()) {
            teacherTestRepository.deleteAll(teacherTests);
        }
        
        // 4. Delete parent-student relationships where user is parent
        var parentLinks = parentStudentRepository.findByParent_UserId(userId);
        if (!parentLinks.isEmpty()) {
            parentStudentRepository.deleteAll(parentLinks);
        }
        
        // 5. Delete parent-student relationships where user is student
        var studentParentLinks = parentStudentRepository.findByStudent_UserId(userId);
        if (!studentParentLinks.isEmpty()) {
            parentStudentRepository.deleteAll(studentParentLinks);
        }
        
        // 6. Delete teacher-student relationships where user is teacher
        var teacherLinks = teacherStudentRepository.findByTeacher_UserId(userId);
        if (!teacherLinks.isEmpty()) {
            teacherStudentRepository.deleteAll(teacherLinks);
        }
        
        // 7. Delete teacher-student relationships where user is student
        var studentTeacherLinks = teacherStudentRepository.findByStudent_UserId(userId);
        if (!studentTeacherLinks.isEmpty()) {
            teacherStudentRepository.deleteAll(studentTeacherLinks);
        }
        
        // 8. Delete student record if user has one
        var studentRecord = studentRepository.findByUser(user);
        if (studentRecord.isPresent()) {
            studentRepository.delete(studentRecord.get());
        }
        
        // 9. Finally, delete the user
        userRepository.deleteById(userId);
    }
}

// Made with Bob