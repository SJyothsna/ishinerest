package com.ishine.ishinerest.auth;

import com.ishine.ishinerest.auth.dto.LoginRequest;
import com.ishine.ishinerest.auth.dto.LoginResponse;
import com.ishine.ishinerest.auth.dto.SignupRequest;
import com.ishine.ishinerest.auth.dto.SignupResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000","http://localhost:5173"}, allowCredentials = "true")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(@Valid @RequestBody SignupRequest req) {
        var s = authService.signup(req);
        return new SignupResponse(s.getStudentId(), s.getName(), s.getEmail());
    }
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        var s = authService.login(req);
        return new LoginResponse(s.getStudentId(), s.getName(), s.getEmail());
    }
}
