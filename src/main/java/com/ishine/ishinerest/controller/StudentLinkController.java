package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.pojo.ParentStudentLinkDTO;
import com.ishine.ishinerest.service.ParentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for Student link request management
 */
@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}, allowCredentials = "true")
public class StudentLinkController {
    
    private final ParentService parentService;
    
    /**
     * Get pending parent link requests for a student
     * GET /students/{studentUserId}/link-requests
     */
    @GetMapping("/{studentUserId}/link-requests")
    public List<ParentStudentLinkDTO> getPendingLinkRequests(@PathVariable Long studentUserId) {
        return parentService.getPendingLinkRequests(studentUserId).stream()
                .map(ParentStudentLinkDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Approve a parent link request
     * POST /students/link-requests/{parentUserId}/approve
     */
    @PostMapping("/link-requests/{parentUserId}/approve")
    @ResponseStatus(HttpStatus.OK)
    public ParentStudentLinkDTO approveLinkRequest(
            @PathVariable Long parentUserId,
            @Valid @RequestBody ApproveRequest request) {
        var link = parentService.approveLinkRequest(parentUserId, request.studentUserId());
        return ParentStudentLinkDTO.fromEntity(link);
    }
    
    /**
     * Reject a parent link request
     * POST /students/link-requests/{parentUserId}/reject
     */
    @PostMapping("/link-requests/{parentUserId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectLinkRequest(
            @PathVariable Long parentUserId,
            @Valid @RequestBody RejectRequest request) {
        parentService.rejectLinkRequest(parentUserId, request.studentUserId());
    }
    
    /**
     * Request DTO for approve action
     */
    public record ApproveRequest(
        @NotNull Long studentUserId
    ) {}
    
    /**
     * Request DTO for reject action
     */
    public record RejectRequest(
        @NotNull Long studentUserId
    ) {}
}

// Made with Bob