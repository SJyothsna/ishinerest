package com.ishine.ishinerest.service;

import com.ishine.ishinerest.entity.Student;
import com.ishine.ishinerest.entity.SubjectEntity;
import com.ishine.ishinerest.entity.TeacherStudent;
import com.ishine.ishinerest.entity.TeacherStudentId;
import com.ishine.ishinerest.entity.TeacherSubject;
import com.ishine.ishinerest.entity.User;
import com.ishine.ishinerest.entity.UserRole;
import com.ishine.ishinerest.pojo.AddStudentByEmailRequest;
import com.ishine.ishinerest.pojo.AddStudentByEmailResponse;
import com.ishine.ishinerest.pojo.StudentSelectedSubjectDTO;
import com.ishine.ishinerest.pojo.TeacherSelectedSubjectDTO;
import com.ishine.ishinerest.pojo.UserDTO;
import com.ishine.ishinerest.repository.StudentRepository;
import com.ishine.ishinerest.repository.SubjectRepository;
import com.ishine.ishinerest.repository.TeacherStudentRepository;
import com.ishine.ishinerest.repository.TeacherSubjectRepository;
import com.ishine.ishinerest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing teacher-student relationships
 */
@Service
@RequiredArgsConstructor
public class TeacherService {
    
    private final TeacherStudentRepository teacherStudentRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Link a teacher to a student
     * @param teacherUserId The user ID of the teacher
     * @param studentUserId The user ID of the student
     */
    @Transactional
    public TeacherStudent linkTeacherToStudent(Long teacherUserId, Long studentUserId) {
        // Verify teacher exists and has TEACHER role
        var teacher = userRepository.findById(teacherUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
        
        if (teacher.getRole() != UserRole.TEACHER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a teacher");
        }
        
        // Verify student user exists and has STUDENT role
        var studentUser = userRepository.findById(studentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        
        if (studentUser.getRole() != UserRole.STUDENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a student");
        }
        
        // Check if relationship already exists
        var id = new TeacherStudentId(teacherUserId, studentUserId);
        if (teacherStudentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Teacher-student relationship already exists");
        }
        
        // Create relationship
        var teacherStudent = new TeacherStudent();
        teacherStudent.setId(id);
        teacherStudent.setTeacher(teacher);
        teacherStudent.setStudent(studentUser);
        
        return teacherStudentRepository.save(teacherStudent);
    }

    /**
     * Add a student to a teacher using name and email.
     * If a student with the email exists, link it.
     * If not, create a guest student account and link it.
     */
    @Transactional
    public AddStudentByEmailResponse addOrLinkStudent(Long teacherUserId, AddStudentByEmailRequest request) {
        validateTeacher(teacherUserId);

        var existingUser = userRepository.findByEmailIgnoreCase(request.email());
        if (existingUser.isPresent()) {
            var user = existingUser.get();
            if (user.getRole() != UserRole.STUDENT) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email exists but user is not a student");
            }

            boolean alreadyLinked = isTeacherLinkedToStudent(teacherUserId, user.getUserId());
            if (!alreadyLinked) {
                linkTeacherToStudent(teacherUserId, user.getUserId());
            }

            return new AddStudentByEmailResponse(
                    false,
                    true,
                    UserDTO.fromEntity(user),
                    null
            );
        }

        String temporaryPassword = generateTemporaryPassword();
        String passwordHash = passwordEncoder.encode(temporaryPassword);

        var newUser = new User();
        newUser.setName(request.name());
        newUser.setEmail(request.email());
        newUser.setPasswordHash(passwordHash);
        newUser.setRole(UserRole.STUDENT);
        newUser.setIsActive(true);
        newUser = userRepository.save(newUser);

        var student = new Student();
        student.setUser(newUser);
        // No need to set name, email, passwordHash - they're accessed via user relationship
        studentRepository.save(student);

        linkTeacherToStudent(teacherUserId, newUser.getUserId());

        return new AddStudentByEmailResponse(
                true,
                true,
                UserDTO.fromEntity(newUser),
                temporaryPassword
        );
    }
    
    /**
     * Unlink a teacher from a student
     */
    @Transactional
    public void unlinkTeacherFromStudent(Long teacherUserId, Long studentUserId) {
        var id = new TeacherStudentId(teacherUserId, studentUserId);
        
        if (!teacherStudentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher-student relationship not found");
        }
        
        teacherStudentRepository.deleteById(id);
    }
    
    /**
     * Get all student users for a teacher
     */
    @Transactional(readOnly = true)
    public List<User> getStudentsForTeacher(Long teacherUserId) {
        return teacherStudentRepository.findByTeacher_UserId(teacherUserId)
                .stream()
                .map(TeacherStudent::getStudent)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all teachers for a student user
     */
    @Transactional(readOnly = true)
    public List<User> getTeachersForStudent(Long studentUserId) {
        return teacherStudentRepository.findByStudent_UserId(studentUserId)
                .stream()
                .map(TeacherStudent::getTeacher)
                .collect(Collectors.toList());
    }
    
    /**
     * Check if a teacher is linked to a student
     */
    @Transactional(readOnly = true)
    public boolean isTeacherLinkedToStudent(Long teacherUserId, Long studentUserId) {
        var id = new TeacherStudentId(teacherUserId, studentUserId);
        return teacherStudentRepository.existsById(id);
    }
    
    /**
     * Get all teacher-student relationships
     */
    @Transactional(readOnly = true)
    public List<TeacherStudent> getAllTeacherStudentRelationships() {
        return teacherStudentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<StudentSelectedSubjectDTO> getTeacherSubjects(Long teacherUserId) {
        validateTeacher(teacherUserId);

        List<TeacherSubject> teacherSubjects = teacherSubjectRepository.findByTeacherUserId(teacherUserId);
        List<String> subjectIds = teacherSubjects.stream()
                .map(TeacherSubject::getSubjectId)
                .toList();

        List<SubjectEntity> subjects = subjectRepository.findBySubjectIdIn(subjectIds);

        return subjects.stream()
                .map(s -> new StudentSelectedSubjectDTO(s.getSubjectId(), s.getSubjectName()))
                .toList();
    }

    @Transactional
    public List<TeacherSelectedSubjectDTO> saveTeacherSubjects(Long teacherUserId, List<String> subjectIds) {
        validateTeacher(teacherUserId);
        validateSubjectIds(subjectIds);

        subjectIds.stream()
                .filter(subjectId -> !teacherSubjectRepository.existsByTeacherUserIdAndSubjectId(teacherUserId, subjectId))
                .forEach(subjectId -> {
                    TeacherSubject teacherSubject = new TeacherSubject();
                    teacherSubject.setTeacherUserId(teacherUserId);
                    teacherSubject.setSubjectId(subjectId);
                    teacherSubjectRepository.save(teacherSubject);
                });

        return getTeacherSubjectsWithClasses(teacherUserId);
    }

    @Transactional
    public void replaceTeacherSubjects(Long teacherUserId, List<String> subjectIds) {
        validateTeacher(teacherUserId);
        validateSubjectIds(subjectIds);
        teacherSubjectRepository.deleteByTeacherUserId(teacherUserId);
        saveTeacherSubjects(teacherUserId, subjectIds);
    }

    @Transactional
    public void deleteTeacherSubject(Long teacherUserId, String subjectId) {
        validateTeacher(teacherUserId);

        if (!teacherSubjectRepository.existsByTeacherUserIdAndSubjectId(teacherUserId, subjectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher subject not found");
        }

        teacherSubjectRepository.deleteByTeacherUserIdAndSubjectId(teacherUserId, subjectId);
    }

    @Transactional(readOnly = true)
    public boolean teacherHasSubject(Long teacherUserId, String subjectId) {
        validateTeacher(teacherUserId);
        return teacherSubjectRepository.existsByTeacherUserIdAndSubjectId(teacherUserId, subjectId);
    }

    @Transactional(readOnly = true)
    public List<TeacherSelectedSubjectDTO> getTeacherSubjectsWithClasses(Long teacherUserId) {
        validateTeacher(teacherUserId);

        List<TeacherSubject> teacherSubjects = teacherSubjectRepository.findByTeacherUserId(teacherUserId);
        List<String> subjectIds = teacherSubjects.stream()
                .map(TeacherSubject::getSubjectId)
                .toList();

        List<SubjectEntity> subjects = subjectRepository.findBySubjectIdIn(subjectIds);

        return subjects.stream()
                .map(subject -> new TeacherSelectedSubjectDTO(
                        subject.getSubjectId(),
                        subject.getSubjectName(),
                        subject.getClasses().stream()
                                .map(classEntity -> new TeacherSelectedSubjectDTO.ClassSummaryDTO(
                                        classEntity.getClassId(),
                                        classEntity.getClassName(),
                                        classEntity.getExam(),
                                        classEntity.getExamId()
                                ))
                                .toList()
                ))
                .toList();
    }

    private User validateTeacher(Long teacherUserId) {
        var teacher = userRepository.findById(teacherUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));

        if (teacher.getRole() != UserRole.TEACHER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a teacher");
        }

        return teacher;
    }

    private String generateTemporaryPassword() {
        return "Temp@" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private void validateSubjectIds(List<String> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one subjectId is required");
        }

        List<SubjectEntity> subjects = subjectRepository.findBySubjectIdIn(subjectIds);
        if (subjects.size() != subjectIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more subjectIds are invalid");
        }
    }
}

 // Made with Bob