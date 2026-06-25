package com.ishine.ishinerest.auth;

import com.ishine.ishinerest.auth.dto.ForgotPasswordRequest;
import com.ishine.ishinerest.auth.dto.LoginRequest;
import com.ishine.ishinerest.auth.dto.LoginResponse;
import com.ishine.ishinerest.auth.dto.ResetPasswordRequest;
import com.ishine.ishinerest.auth.dto.SignupRequest;
import com.ishine.ishinerest.auth.dto.SignupResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000","http://localhost:5173"}, allowCredentials = "true")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(@Valid @RequestBody SignupRequest req) {
        return authService.signup(req);
    }
    
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }
    
    /**
     * Request password reset - sends email with reset token
     */
    @PostMapping("/forgot-password")
    public Map<String, Object> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req,
                                              HttpServletRequest request) {
        return authService.requestPasswordReset(req, request);
    }
    
    /**
     * Validate password reset token
     */
    @GetMapping("/validate-reset-token")
    public Map<String, Object> validateResetToken(@RequestParam String token) {
        return authService.validateResetToken(token);
    }
    
    /**
     * Reset password with token
     */
    @PostMapping("/reset-password")
    public Map<String, Object> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        return authService.resetPassword(req);
    }
    
    /**
     * Verify email with token
     */
    @GetMapping("/verify-email")
    public Map<String, Object> verifyEmail(@RequestParam String token) {
        return authService.verifyEmail(token);
    }
    
    /**
     * Resend verification email
     */
    @PostMapping("/resend-verification")
    public Map<String, Object> resendVerification(@RequestParam String email) {
        return authService.resendVerificationEmail(email);
    }
}
