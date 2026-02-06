package com.ishine.ishinerest.auth;

import com.ishine.ishinerest.auth.dto.LoginRequest;
import com.ishine.ishinerest.auth.dto.SignupRequest;
import com.ishine.ishinerest.entity.Student;
import com.ishine.ishinerest.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Student signup(SignupRequest req) {
        if (studentRepository.existsByEmail(req.email())) {
            throw new EmailInUseException("Email already in use");
        }
        var s = new Student();
        s.setName(req.name());
        s.setEmail(req.email());
        s.setPasswordHash(passwordEncoder.encode(req.password()));
        // s.setClassEntity(null); // student can choose class later
        return studentRepository.save(s);
    }
    public Student login(LoginRequest req) {
        var student = studentRepository.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), student.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return student;
    }
}
