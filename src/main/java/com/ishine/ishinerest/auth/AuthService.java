package com.ishine.ishinerest.auth;

import com.ishine.ishinerest.auth.dto.LoginRequest;
import com.ishine.ishinerest.auth.dto.LoginResponse;
import com.ishine.ishinerest.auth.dto.SignupRequest;
import com.ishine.ishinerest.auth.dto.SignupResponse;
import com.ishine.ishinerest.entity.Student;
import com.ishine.ishinerest.entity.User;
import com.ishine.ishinerest.entity.UserRole;
import com.ishine.ishinerest.repository.StudentRepository;
import com.ishine.ishinerest.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Multi-role signup: Creates a User and optionally a Student record
     * @param req SignupRequest with name, email, password, and role
     * @return SignupResponse with user details
     */
    @Transactional
    public SignupResponse signup(SignupRequest req) {
        // Validate role (no ADMIN signup through this endpoint)
        if (req.role() == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin accounts cannot be created through signup");
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(req.email())) {
            throw new EmailInUseException("Email already in use");
        }
        
        // Create User entity
        var user = new User();
        user.setName(req.name());
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(req.role());
        user.setIsActive(true);
        user = userRepository.save(user);
        
        // If role is STUDENT, also create Student record
        Long studentId = null;
        if (req.role() == UserRole.STUDENT) {
            var student = new Student();
            student.setUser(user);  // Set user first - @MapsId will use user.userId as studentId
            // No need to set name, email, passwordHash - they're accessed via user relationship
            student = studentRepository.save(student);
            studentId = student.getStudentId();  // Will be same as user.getUserId()
        }
        
        return new SignupResponse(
            user.getUserId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            studentId  // studentId equals userId
        );
    }

    /**
     * Multi-role login: Authenticates user and returns user details
     * @param req LoginRequest with email and password
     * @return LoginResponse with user details
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        // Find user by email
        var user = userRepository.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        
        // Check if user is active
        if (!user.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is deactivated");
        }
        
        // Verify password
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        
        // Get student ID if user is a student
        Long studentId = null;
        if (user.getRole() == UserRole.STUDENT) {
            var student = studentRepository.findByUser(user);
            if (student.isPresent()) {
                studentId = student.get().getStudentId();  // Will be same as userId due to @MapsId
            }
        }
        
        return new LoginResponse(
            user.getUserId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            studentId  // studentId equals userId
        );
    }
}
