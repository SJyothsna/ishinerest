package com.istudy.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final LocalAuthService auth;
  private final CurrentStudent current;
  private final boolean emailEnabled;

  public AuthController(
    LocalAuthService auth,
    CurrentStudent current,
    @Value("${app.mail.enabled:false}") boolean emailEnabled
  ) {
    this.auth = auth;
    this.current = current;
    this.emailEnabled = emailEnabled;
  }

  public record Login(
    @NotBlank @Email String email,
    @NotBlank @Size(max = 72) String password
  ) {}

  public record Signup(
    @NotBlank String name,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 72) String password,
    @NotBlank String role
  ) {}

  @PostMapping("/login")
  public Map<String, Object> login(@Valid @RequestBody Login request) {
    return auth.login(request.email(), request.password());
  }

  @PostMapping("/signup")
  public Map<String, Object> signup(@Valid @RequestBody Signup request) {
    return auth.signup(
      request.name(),
      request.email(),
      request.password(),
      request.role()
    );
  }

  @PostMapping("/logout")
  public void logout(@RequestHeader("Authorization") String header) {
    auth.logout(header.substring(7));
  }

  @GetMapping("/me")
  public Map<String, Object> me() {
    return auth.profile(current.id());
  }

  @GetMapping("/config")
  public Map<String, Object> config() {
    return Map.of(
      "provider",
      "LOCAL",
      "googleEnabled",
      false,
      "schoolOnboardingEnabled",
      false,
      "emailVerificationEnabled",
      emailEnabled
    );
  }
}
