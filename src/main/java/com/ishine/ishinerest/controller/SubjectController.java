package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.entity.ExamQuestion;
import com.ishine.ishinerest.entity.SubjectEntity;
import com.ishine.ishinerest.service.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subjects")
public class SubjectController {

    @Autowired
    private SubjectService subjectService;

    @GetMapping("/class/{classId}")
    public List<SubjectEntity> getSubjectsByClass(@PathVariable String classId) {
        return subjectService.getSubjectsByClass(classId);
    }

    @PostMapping
    public List<SubjectEntity> saveSubjects(@RequestBody List<SubjectEntity> subjects) {
        return subjectService.saveSubjects(subjects);
    }

    // Update a subject
    @PutMapping("/{subjectId}")
    public SubjectEntity updateSubject(@PathVariable String subjectId, @RequestBody SubjectEntity subjectDetails) {
        return subjectService.updateSubject(subjectId, subjectDetails);
    }

    // Delete a subject
    @DeleteMapping("/{subjectId}")
    public String deleteSubject(@PathVariable String subjectId) {
        subjectService.deleteSubject(subjectId);
        return "Subject with ID " + subjectId + " has been deleted.";
    }

}
