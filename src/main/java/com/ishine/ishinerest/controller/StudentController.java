package com.ishine.ishinerest.controller;

import com.ishine.ishinerest.entity.Student;
import com.ishine.ishinerest.entity.StudentSubject;
import com.ishine.ishinerest.pojo.StudentPracticeProgressDTO;
import com.ishine.ishinerest.pojo.StudentSelectedSubjectDTO;
import com.ishine.ishinerest.repository.PracticeSessionDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ishine.ishinerest.service.StudentService;


import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/students")
public class StudentController {

    @Autowired
    private StudentService studentService;
    @Autowired
    private PracticeSessionDetailRepository practiceSessionDetailRepository;

    @GetMapping
    public List<Student> getAllStudents() {
        return studentService.getAllStudents();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Student> getStudentById(@PathVariable Long id) {
        Optional<Student> student = studentService.getStudentById(id);
        return student.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Student createStudent(@RequestBody Student student) {
        return studentService.saveStudent(student);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{studentId}/practiceProgress/chapter")
    public ResponseEntity<StudentPracticeProgressDTO> getChapterPracticeProgress(
            @PathVariable Long studentId,
            @RequestParam String chapterId
    ) {
        return ResponseEntity.ok(studentService.getPracticeProgressByChapter(studentId, chapterId));
    }

    @GetMapping("/{studentId}/practiceProgress/subject")
    public ResponseEntity<StudentPracticeProgressDTO> getSubjectPracticeProgress(
            @PathVariable Long studentId,
            @RequestParam String subjectId
    ) {
        return ResponseEntity.ok(studentService.getPracticeProgressBySubject(studentId, subjectId));
    }
    @GetMapping("/{studentId}/subjects")
    public ResponseEntity<List<StudentSelectedSubjectDTO>> getStudentSubjects(@PathVariable Long studentId) {
        List<StudentSelectedSubjectDTO> selections = studentService.getSubjectsSelectedByStudent(studentId);
        return ResponseEntity.ok(selections);
    }

    @PostMapping("/{studentId}/subjects")
    public ResponseEntity<List<StudentSubject>> selectSubjectsForStudent(
            @PathVariable Long studentId,
            @RequestBody List<String> subjectIds
    ) {
        List<StudentSubject> savedSelections =
                studentService.saveStudentSubjects(studentId, subjectIds);
        return ResponseEntity.ok(savedSelections);
    }


}