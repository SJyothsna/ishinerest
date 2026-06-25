package com.ishine.ishinerest.auth;

import com.ishine.ishinerest.auth.dto.LoginRequest;
import com.ishine.ishinerest.auth.dto.LoginResponse;
import com.ishine.ishinerest.auth.dto.SignupRequest;
import com.ishine.ishinerest.auth.dto.SignupResponse;
import com.ishine.ishinerest.auth.dto.ForgotPasswordRequest;
import com.ishine.ishinerest.auth.dto.ResetPasswordRequest;
import com.ishine.ishinerest.entity.PasswordResetToken;
import com.ishine.ishinerest.entity.Student;
import com.ishine.ishinerest.entity.User;
import com.ishine.ishinerest.entity.UserRole;
import com.ishine.ishinerest.repository.PasswordResetTokenRepository;
import com.ishine.ishinerest.repository.StudentRepository;
import com.ishine.ishinerest.repository.UserRepository;
import com.ishine.ishinerest.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    
    // Cache to track recently verified tokens (token -> timestamp)
    // This prevents 400 errors when the verification link is clicked multiple times
    private final Map<String, LocalDateTime> recentlyVerifiedTokens = new ConcurrentHashMap<>();

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
        
        // Generate verification token
        String verificationToken = generateSecureToken();
        
        // Create User entity
        var user = new User();
        user.setName(req.name());
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(req.role());
        user.setIsActive(true);
        user.setEmailVerified(false);
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24)); // 24 hour expiry
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
        
        // Send verification email asynchronously
        emailService.sendVerificationEmail(user.getEmail(), user.getName(), verificationToken);
        log.info("Verification email sent to: {}", user.getEmail());
        
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
            studentId,  // studentId equals userId
            user.getEmailVerified()  // Email verification status
        );
    }

    /**
     * Request password reset - sends email with reset token
     * @param req ForgotPasswordRequest with email
     * @param request HttpServletRequest for IP and user agent
     * @return Map with success message
     */
    @Transactional
    public Map<String, Object> requestPasswordReset(ForgotPasswordRequest req, HttpServletRequest request) {
        // Find user by email (case-insensitive)
        var userOpt = userRepository.findByEmailIgnoreCase(req.getEmail());
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Generate secure token
            String token = generateSecureToken();
            
            // Delete any existing tokens for this user
            passwordResetTokenRepository.deleteByUserId(user.getUserId());
            
            // Create new token
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUserId(user.getUserId());
            resetToken.setToken(token);
            resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
            resetToken.setIpAddress(getClientIp(request));
            resetToken.setUserAgent(request.getHeader("User-Agent"));
            passwordResetTokenRepository.save(resetToken);
            
            // Send email asynchronously
            emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token);
            
            log.info("Password reset requested for user: {}", user.getEmail());
        }
        
        // Always return success (security: don't reveal if email exists)
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "If an account exists with this email, you will receive a password reset link shortly.");
        return response;
    }
    
    /**
     * Validate reset token
     * @param token Reset token from email
     * @return Map with validation result and email
     */
    @Transactional(readOnly = true)
    public Map<String, Object> validateResetToken(String token) {
        var tokenOpt = passwordResetTokenRepository.findByToken(token);
        
        if (tokenOpt.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("valid", false);
            response.put("error", "INVALID_TOKEN");
            response.put("message", "Invalid reset link.");
            return response;
        }
        
        PasswordResetToken resetToken = tokenOpt.get();
        
        // Check if already used
        if (resetToken.isUsed()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("valid", false);
            response.put("error", "TOKEN_USED");
            response.put("message", "This reset link has already been used.");
            return response;
        }
        
        // Check if expired
        if (resetToken.isExpired()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("valid", false);
            response.put("error", "TOKEN_EXPIRED");
            response.put("message", "This reset link has expired. Please request a new one.");
            return response;
        }
        
        // Get user email
        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("valid", true);
        response.put("email", user.getEmail());
        return response;
    }
    
    /**
     * Reset password with token
     * @param req ResetPasswordRequest with token and new password
     * @return Map with success message
     */
    @Transactional
    public Map<String, Object> resetPassword(ResetPasswordRequest req) {
        var tokenOpt = passwordResetTokenRepository.findByToken(req.getToken());
        
        if (tokenOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reset link.");
        }
        
        PasswordResetToken resetToken = tokenOpt.get();
        
        // Check if already used
        if (resetToken.isUsed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This reset link has already been used.");
        }
        
        // Check if expired
        if (resetToken.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This reset link has expired.");
        }
        
        // Get user
        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        // Hash and update password
        String hashedPassword = passwordEncoder.encode(req.getNewPassword());
        user.setPasswordHash(hashedPassword);
        userRepository.save(user);
        
        // Mark token as used
        resetToken.setUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);
        
        log.info("Password reset successfully for user: {}", user.getEmail());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Password reset successfully. You can now log in with your new password.");
        return response;
    }
    
    /**
     * Verify email with token
     * @param token Verification token from email
     * @return Map with success message
     */
    @Transactional
    public Map<String, Object> verifyEmail(String token) {
        // Validate token parameter
        if (token == null || token.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification token is required.");
        }
        
        // Check if this token was recently verified (within last 5 minutes)
        // This handles duplicate requests (e.g., double-clicking the link)
        LocalDateTime recentVerification = recentlyVerifiedTokens.get(token);
        if (recentVerification != null && LocalDateTime.now().minusMinutes(5).isBefore(recentVerification)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email already verified. You can log in now.");
            response.put("alreadyVerified", true);
            log.info("Duplicate verification request detected for token (recently verified)");
            return response;
        }
        
        // Find user by verification token
        var userOpt = userRepository.findByVerificationToken(token);
        
        if (userOpt.isEmpty()) {
            // Token not found - could be invalid or already used
            // Check if it was recently verified
            if (recentVerification != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Email already verified. You can log in now.");
                response.put("alreadyVerified", true);
                return response;
            }
            
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid or expired verification link. If you just verified your email, you can log in now.");
        }
        
        User user = userOpt.get();
        
        // Check if already verified
        if (user.getEmailVerified()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email already verified. You can log in now.");
            response.put("alreadyVerified", true);
            return response;
        }
        
        // Check if token expired
        if (user.getVerificationTokenExpiry() != null &&
            LocalDateTime.now().isAfter(user.getVerificationTokenExpiry())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Verification link has expired. Please request a new one.");
        }
        
        // Mark email as verified
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setVerificationToken(null);  // Clear token after use
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);
        
        // Add to recently verified cache
        recentlyVerifiedTokens.put(token, LocalDateTime.now());
        
        // Clean up old entries from cache (older than 10 minutes)
        cleanupRecentlyVerifiedCache();
        
        log.info("Email verified successfully for user: {}", user.getEmail());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Email verified successfully! You can now access all features.");
        response.put("alreadyVerified", false);
        return response;
    }
    
    /**
     * Clean up old entries from recently verified tokens cache
     */
    private void cleanupRecentlyVerifiedCache() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
        recentlyVerifiedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
    }
    
    /**
     * Resend verification email
     * @param email User's email address
     * @return Map with success message
     */
    @Transactional
    public Map<String, Object> resendVerificationEmail(String email) {
        // Find user by email
        var userOpt = userRepository.findByEmailIgnoreCase(email);
        
        if (userOpt.isEmpty()) {
            // Don't reveal if email exists (security)
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "If an account exists with this email, a verification link has been sent.");
            return response;
        }
        
        User user = userOpt.get();
        
        // Check if already verified
        if (user.getEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already verified.");
        }
        
        // Generate new verification token
        String verificationToken = generateSecureToken();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);
        
        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), user.getName(), verificationToken);
        log.info("Verification email resent to: {}", user.getEmail());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Verification email sent. Please check your inbox.");
        return response;
    }
    
    /**
     * Generate secure random token
     */
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return bytesToHex(bytes);
    }
    
    /**
     * Convert bytes to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Get client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
