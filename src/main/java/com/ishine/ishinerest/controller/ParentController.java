package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.entity.ParentStudent.LinkStatus;
import com.ishine.ishinerest.pojo.LinkStudentByEmailRequest;
import com.ishine.ishinerest.pojo.LinkStudentRequest;
import com.ishine.ishinerest.pojo.ParentStudentLinkDTO;
import com.ishine.ishinerest.pojo.UserDTO;
import com.ishine.ishinerest.service.ParentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for Parent-Student relationship management
 */
@RestController
@RequestMapping("/parents")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}, allowCredentials = "true")
public class ParentController {
    
    private final ParentService parentService;
    
    /**
     * Link a parent to a student by email (creates pending request)
     * POST /api/parents/{parentUserId}/link-student
     */
    @PostMapping("/{parentUserId}/link-student")
    @ResponseStatus(HttpStatus.CREATED)
    public ParentStudentLinkDTO linkParentToStudentByEmail(
            @PathVariable Long parentUserId,
            @Valid @RequestBody LinkStudentByEmailRequest request) {
        var link = parentService.linkParentToStudentByEmail(
            parentUserId,
            request.studentEmail(),
            request.relationshipType()
        );
        return ParentStudentLinkDTO.fromEntity(link);
    }
    
    /**
     * Link a parent to a student by ID (backward compatibility)
     * POST /api/parents/{parentUserId}/students
     */
    @PostMapping("/{parentUserId}/students")
    @ResponseStatus(HttpStatus.CREATED)
    public void linkParentToStudent(
            @PathVariable Long parentUserId,
            @Valid @RequestBody LinkStudentRequest request) {
        parentService.linkParentToStudent(parentUserId, request.studentUserId());
    }
    
    /**
     * Unlink a parent from a student (revoke access)
     * DELETE /api/parents/{parentUserId}/students/{studentUserId}
     */
    @DeleteMapping("/{parentUserId}/students/{studentUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlinkParentFromStudent(
            @PathVariable Long parentUserId,
            @PathVariable Long studentUserId) {
        parentService.unlinkParentFromStudent(parentUserId, studentUserId);
    }
    
    /**
     * Get all students for a parent (active links only)
     * GET /api/parents/{parentUserId}/students
     */
    @GetMapping("/{parentUserId}/students")
    public List<UserDTO> getStudentsForParent(@PathVariable Long parentUserId) {
        return parentService.getStudentsForParent(parentUserId).stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all parent-student links with optional status filter
     * GET /api/parents/{parentUserId}/links?status=ACTIVE
     */
    @GetMapping("/{parentUserId}/links")
    public List<ParentStudentLinkDTO> getLinksForParent(
            @PathVariable Long parentUserId,
            @RequestParam(required = false) LinkStatus status) {
        return parentService.getLinksForParent(parentUserId, status).stream()
                .map(ParentStudentLinkDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all parents for a student
     * GET /api/students/{studentUserId}/parents
     */
    @GetMapping("/students/{studentUserId}/parents")
    public List<UserDTO> getParentsForStudent(@PathVariable Long studentUserId) {
        return parentService.getParentsForStudent(studentUserId).stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Check if a parent is actively linked to a student
     * GET /api/parents/{parentUserId}/students/{studentUserId}/linked
     */
    @GetMapping("/{parentUserId}/students/{studentUserId}/linked")
    public boolean isParentLinkedToStudent(
            @PathVariable Long parentUserId,
            @PathVariable Long studentUserId) {
        return parentService.isParentLinkedToStudent(parentUserId, studentUserId);
    }
}

// Made with Bob