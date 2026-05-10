package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.pojo.AddStudentByEmailRequest;
import com.ishine.ishinerest.pojo.AddStudentByEmailResponse;
import com.ishine.ishinerest.pojo.LinkStudentRequest;
import com.ishine.ishinerest.pojo.TeacherSelectedSubjectDTO;
import com.ishine.ishinerest.pojo.TeacherSubjectSelectionRequest;
import com.ishine.ishinerest.pojo.UserDTO;
import com.ishine.ishinerest.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for Teacher-Student relationship management
 */
@RestController
@RequestMapping("/teachers")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}, allowCredentials = "true")
public class TeacherController {
    
    private final TeacherService teacherService;
    
    /**
     * Link a teacher to a student
     * POST /teachers/{teacherUserId}/students
     */
    @PostMapping("/{teacherUserId}/students")
    @ResponseStatus(HttpStatus.CREATED)
    public void linkTeacherToStudent(
            @PathVariable Long teacherUserId,
            @Valid @RequestBody LinkStudentRequest request) {
        teacherService.linkTeacherToStudent(teacherUserId, request.studentUserId());
    }

    /**
     * Add a student by name and email.
     * If the email already belongs to a student, link that student.
     * Otherwise create a guest student account and link it.
     * POST /teachers/{teacherUserId}/students/add-or-link
     */
    @PostMapping("/{teacherUserId}/students/add-or-link")
    @ResponseStatus(HttpStatus.CREATED)
    public AddStudentByEmailResponse addOrLinkStudent(
            @PathVariable Long teacherUserId,
            @Valid @RequestBody AddStudentByEmailRequest request) {
        return teacherService.addOrLinkStudent(teacherUserId, request);
    }
    
    /**
     * Unlink a teacher from a student
     * DELETE /teachers/{teacherUserId}/students/{studentUserId}
     */
    @DeleteMapping("/{teacherUserId}/students/{studentUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlinkTeacherFromStudent(
            @PathVariable Long teacherUserId,
            @PathVariable Long studentUserId) {
        teacherService.unlinkTeacherFromStudent(teacherUserId, studentUserId);
    }
    
    /**
     * Get all students for a teacher
     * GET /teachers/{teacherUserId}/students
     */
    @GetMapping("/{teacherUserId}/students")
    public List<UserDTO> getStudentsForTeacher(@PathVariable Long teacherUserId) {
        return teacherService.getStudentsForTeacher(teacherUserId).stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all teachers for a student
     * GET /students/{studentUserId}/teachers
     */
    @GetMapping("/students/{studentUserId}/teachers")
    public List<UserDTO> getTeachersForStudent(@PathVariable Long studentUserId) {
        return teacherService.getTeachersForStudent(studentUserId).stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Check if a teacher is linked to a student
     * GET /teachers/{teacherUserId}/students/{studentUserId}/linked
     */
    @GetMapping("/{teacherUserId}/students/{studentUserId}/linked")
    public boolean isTeacherLinkedToStudent(
            @PathVariable Long teacherUserId,
            @PathVariable Long studentUserId) {
        return teacherService.isTeacherLinkedToStudent(teacherUserId, studentUserId);
    }
    /**
     * Get selected subjects for a teacher
     * GET /teachers/{teacherUserId}/subjects
     */
    @GetMapping("/{teacherUserId}/subjects")
    public List<TeacherSelectedSubjectDTO> getTeacherSubjects(@PathVariable Long teacherUserId) {
        return teacherService.getTeacherSubjectsWithClasses(teacherUserId);
    }

    /**
     * Add selected subjects for a teacher
     * POST /teachers/{teacherUserId}/subjects
     */
    @PostMapping("/{teacherUserId}/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    public List<TeacherSelectedSubjectDTO> saveTeacherSubjects(
            @PathVariable Long teacherUserId,
            @Valid @RequestBody TeacherSubjectSelectionRequest request) {
        return teacherService.saveTeacherSubjects(teacherUserId, request.subjectIds());
    }

    /**
     * Replace selected subjects for a teacher
     * PUT /teachers/{teacherUserId}/subjects
     */
    @PutMapping("/{teacherUserId}/subjects")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void replaceTeacherSubjects(
            @PathVariable Long teacherUserId,
            @Valid @RequestBody TeacherSubjectSelectionRequest request) {
        teacherService.replaceTeacherSubjects(teacherUserId, request.subjectIds());
    }

    /**
     * Delete one selected subject for a teacher
     * DELETE /teachers/{teacherUserId}/subjects/{subjectId}
     */
    @DeleteMapping("/{teacherUserId}/subjects/{subjectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeacherSubject(
            @PathVariable Long teacherUserId,
            @PathVariable String subjectId) {
        teacherService.deleteTeacherSubject(teacherUserId, subjectId);
    }
}

// Made with Bob