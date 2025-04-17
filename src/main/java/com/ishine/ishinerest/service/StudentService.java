package com.ishine.ishinerest.service;


import com.ishine.ishinerest.entity.Student;
import com.ishine.ishinerest.entity.StudentSubject;
import com.ishine.ishinerest.entity.SubjectEntity;
import com.ishine.ishinerest.pojo.StudentPracticeProgressDTO;
import com.ishine.ishinerest.pojo.StudentSelectedSubjectDTO;
import com.ishine.ishinerest.repository.PracticeSessionDetailRepository;
import com.ishine.ishinerest.repository.StudentRepository;
import com.ishine.ishinerest.repository.StudentSubjectRepository;
import com.ishine.ishinerest.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private StudentSubjectRepository studentSubjectRepository;

    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private PracticeSessionDetailRepository practiceSessionDetailRepository;

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public Optional<Student> getStudentById(Long id) {
        return studentRepository.findById(id);
    }

    public Student saveStudent(Student student) {
        return studentRepository.save(student);
    }

    public void deleteStudent(Long id) {
        studentRepository.deleteById(id);
    }
    public StudentPracticeProgressDTO getPracticeProgressByChapter(Long studentId, String chapterId) {
        List<Object[]> results = practiceSessionDetailRepository.getChapterProgress(studentId, chapterId);

        if (results == null || results.isEmpty()) {
            return new StudentPracticeProgressDTO(0L, 0L, 0L, 0L, 0L);
        }

        Object[] row = results.get(0); // ✅ Get the first row
        return mapToProgressDTO(row);
    }

    public StudentPracticeProgressDTO getPracticeProgressBySubject(Long studentId, String subjectId) {
        List<Object[]> resultList = practiceSessionDetailRepository.getSubjectProgress(studentId, subjectId);

        if (resultList == null || resultList.isEmpty()) {
            return new StudentPracticeProgressDTO(0L, 0L, 0L, 0L, 0L);
        }

        Object[] row = resultList.get(0);  // ✅ Extract first row from the result
        return mapToProgressDTO(row);
    }

    private StudentPracticeProgressDTO mapToProgressDTO(Object[] row) {
        Long totalQuestions     = row[0] != null ? ((Number) row[0]).longValue() : 0L;
        Long practicedQuestions = row[1] != null ? ((Number) row[1]).longValue() : 0L;
        Long correctAnswers     = row[2] != null ? ((Number) row[2]).longValue() : 0L;
        Long incorrectAnswers   = row[3] != null ? ((Number) row[3]).longValue() : 0L;
        Long notPracticed       = totalQuestions - practicedQuestions;

        return new StudentPracticeProgressDTO(
                totalQuestions, practicedQuestions, correctAnswers, incorrectAnswers, notPracticed
        );
    }
    public List<StudentSelectedSubjectDTO> getSubjectsSelectedByStudent(Long studentId) {
        List<StudentSubject> studentSubjects = studentSubjectRepository.findByStudentId(studentId);

        List<String> subjectIds = studentSubjects.stream()
                .map(StudentSubject::getSubjectId)
                .toList();

        List<SubjectEntity> subjects = subjectRepository.findBySubjectIdIn(subjectIds);

        return subjects.stream()
                .map(s -> new StudentSelectedSubjectDTO(s.getSubjectId(), s.getSubjectName()))
                .toList();
    }
    public List<StudentSubject> saveStudentSubjects(Long studentId, List<String> subjectIds) {
        List<StudentSubject> savedSubjects = new ArrayList<>();

        for (String subjectId : subjectIds) {
            boolean alreadyExists = studentSubjectRepository.existsByStudentIdAndSubjectId(studentId, subjectId);
            if (!alreadyExists) {
                StudentSubject studentSubject = new StudentSubject();
                studentSubject.setStudentId(studentId);
                studentSubject.setSubjectId(subjectId);
                studentSubject.setSelectedAt(LocalDateTime.now());
                savedSubjects.add(studentSubjectRepository.save(studentSubject));
            }
        }

        return savedSubjects;
    }


}

