package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.ParentStudent;
import com.ishine.ishinerest.entity.ParentStudent.LinkStatus;
import com.ishine.ishinerest.entity.ParentStudentId;
import com.ishine.ishinerest.entity.Student;
import com.ishine.ishinerest.entity.User;
import com.ishine.ishinerest.entity.UserRole;
import com.ishine.ishinerest.repository.ParentStudentRepository;
import com.ishine.ishinerest.repository.StudentRepository;
import com.ishine.ishinerest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing parent-student relationships
 */
@Service
@RequiredArgsConstructor
public class ParentService {
    
    private final ParentStudentRepository parentStudentRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    
    /**
     * Link a parent to a student by email (creates pending request)
     * @param parentUserId The user ID of the parent
     * @param studentEmail The email of the student
     * @param relationshipType Optional relationship type
     */
    @Transactional
    public ParentStudent linkParentToStudentByEmail(Long parentUserId, String studentEmail, String relationshipType) {
        // Verify parent exists and has PARENT role
        var parent = userRepository.findById(parentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent not found"));
        
        if (parent.getRole() != UserRole.PARENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a parent");
        }
        
        // Find student by email
        var studentUser = userRepository.findByEmailIgnoreCase(studentEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No student account exists with this email address"));
        
        if (studentUser.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a student");
        }
        
        // Check if relationship already exists or is pending
        var id = new ParentStudentId(parentUserId, studentUser.getUserId());
        var existing = parentStudentRepository.findById(id);
        if (existing.isPresent()) {
            var status = existing.get().getStatus();
            if (status == LinkStatus.PENDING) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A pending link request already exists for this student");
            } else if (status == LinkStatus.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This student is already linked to your account");
            }
        }
        
        // Create pending relationship
        var parentStudent = new ParentStudent();
        parentStudent.setId(id);
        parentStudent.setParent(parent);
        parentStudent.setStudent(studentUser);
        // Default to "PARENT" if relationshipType is not provided
        parentStudent.setRelationshipType(relationshipType != null ? relationshipType : "PARENT");
        parentStudent.setStatus(LinkStatus.PENDING);
        
        return parentStudentRepository.save(parentStudent);
    }
    
    /**
     * Link a parent to a student (direct link, backward compatibility)
     * @param parentUserId The user ID of the parent
     * @param studentUserId The user ID of the student
     */
    @Transactional
    public ParentStudent linkParentToStudent(Long parentUserId, Long studentUserId) {
        // Verify parent exists and has PARENT role
        var parent = userRepository.findById(parentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent not found"));
        
        if (parent.getRole() != UserRole.PARENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a parent");
        }
        
        // Verify student user exists and has STUDENT role
        var studentUser = userRepository.findById(studentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        
        if (studentUser.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a student");
        }
        
        // Check if relationship already exists
        var id = new ParentStudentId(parentUserId, studentUserId);
        if (parentStudentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Parent-student relationship already exists");
        }
        
        // Create relationship with PENDING status (requires approval)
        var parentStudent = new ParentStudent();
        parentStudent.setId(id);
        parentStudent.setParent(parent);
        parentStudent.setStudent(studentUser);
        parentStudent.setStatus(LinkStatus.PENDING);
        
        return parentStudentRepository.save(parentStudent);
    }
    
    /**
     * Student approves a parent link request
     */
    @Transactional
    public ParentStudent approveLinkRequest(Long parentUserId, Long studentUserId) {
        var link = parentStudentRepository.findByParent_UserIdAndStudent_UserId(parentUserId, studentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Link request not found"));
        
        if (link.getStatus() != LinkStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Link request has already been processed");
        }
        
        link.setStatus(LinkStatus.ACTIVE);
        link.setApprovedAt(LocalDateTime.now());
        return parentStudentRepository.save(link);
    }
    
    /**
     * Student rejects a parent link request
     */
    @Transactional
    public void rejectLinkRequest(Long parentUserId, Long studentUserId) {
        var link = parentStudentRepository.findByParent_UserIdAndStudent_UserId(parentUserId, studentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Link request not found"));
        
        if (link.getStatus() != LinkStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Link request has already been processed");
        }
        
        link.setStatus(LinkStatus.REJECTED);
        link.setRejectedAt(LocalDateTime.now());
        parentStudentRepository.save(link);
    }
    
    /**
     * Get pending link requests for a student
     */
    @Transactional(readOnly = true)
    public List<ParentStudent> getPendingLinkRequests(Long studentUserId) {
        return parentStudentRepository.findByStudent_UserIdAndStatus(studentUserId, LinkStatus.PENDING);
    }
    
    /**
     * Unlink a parent from a student (revoke access)
     */
    @Transactional
    public void unlinkParentFromStudent(Long parentUserId, Long studentUserId) {
        var link = parentStudentRepository.findByParent_UserIdAndStudent_UserId(parentUserId, studentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Parent-student relationship not found"));
        
        // Mark as revoked instead of deleting
        link.setStatus(LinkStatus.REVOKED);
        parentStudentRepository.save(link);
    }
    
    /**
     * Get all student users for a parent (active links only)
     */
    @Transactional(readOnly = true)
    public List<User> getStudentsForParent(Long parentUserId) {
        return parentStudentRepository.findByParent_UserIdAndStatus(parentUserId, LinkStatus.ACTIVE)
                .stream()
                .map(ParentStudent::getStudent)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all parent-student links for a parent (with status filter)
     */
    @Transactional(readOnly = true)
    public List<ParentStudent> getLinksForParent(Long parentUserId, LinkStatus status) {
        if (status != null) {
            return parentStudentRepository.findByParent_UserIdAndStatus(parentUserId, status);
        }
        return parentStudentRepository.findByParent_UserId(parentUserId);
    }
    
    /**
     * Get all parents for a student user (active links only)
     */
    @Transactional(readOnly = true)
    public List<User> getParentsForStudent(Long studentUserId) {
        return parentStudentRepository.findByStudent_UserIdAndStatus(studentUserId, LinkStatus.ACTIVE)
                .stream()
                .map(ParentStudent::getParent)
                .collect(Collectors.toList());
    }
    
    /**
     * Check if a parent is actively linked to a student
     */
    @Transactional(readOnly = true)
    public boolean isParentLinkedToStudent(Long parentUserId, Long studentUserId) {
        return parentStudentRepository.existsByParent_UserIdAndStudent_UserIdAndStatus(
            parentUserId, studentUserId, LinkStatus.ACTIVE);
    }
    
    /**
     * Get all parent-student relationships
     */
    @Transactional(readOnly = true)
    public List<ParentStudent> getAllParentStudentRelationships() {
        return parentStudentRepository.findAll();
    }
}

// Made with Bob