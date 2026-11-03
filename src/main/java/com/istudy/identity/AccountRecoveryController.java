package com.istudy.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AccountRecoveryController {

  private final JdbcTemplate db;
  private final CurrentStudent current;
  private final ObjectProvider<JavaMailSender> mail;
  private final boolean enabled;
  private final String frontend, from;

  public AccountRecoveryController(
    JdbcTemplate db,
    CurrentStudent current,
    ObjectProvider<JavaMailSender> mail,
    @Value("${app.mail.enabled:false}") boolean enabled,
    @Value("${app.frontend-url}") String frontend,
    @Value("${app.mail.from:support@istudy.ie}") String from
  ) {
    this.db = db;
    this.current = current;
    this.mail = mail;
    this.enabled = enabled;
    this.frontend = frontend;
    this.from = from;
  }

  public record Email(
    @NotBlank @jakarta.validation.constraints.Email String email
  ) {}

  public record Token(@NotBlank String token) {}

  public record Password(
    @NotBlank String token,
    @NotBlank @Size(min = 8, max = 72) String password
  ) {}

  private Map<String, Object> token(String raw, String purpose, boolean lock) {
    var rows = db.queryForList(
      "select t.user_id,u.email from account_action_tokens t join app_users u on u.user_id=t.user_id where t.token_hash=? and t.purpose=? and t.expires_at>current_timestamp and t.consumed_at is null and u.status='ACTIVE'" +
      (lock ? " for update" : ""),
      LocalAuthService.hash(raw),
      purpose
    );
    if (rows.isEmpty()) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "This link is invalid or has expired"
    );
    return rows.get(0);
  }

  private Map<String, Object> send(String email, String purpose) {
    if (
      !enabled || mail.getIfAvailable() == null
    ) throw new ResponseStatusException(
      HttpStatus.SERVICE_UNAVAILABLE,
      "Email delivery is not configured in this local environment yet"
    );
    var users = db.queryForList(
      "select user_id from app_users where lower(email)=? and status='ACTIVE'",
      UUID.class,
      email.trim().toLowerCase(Locale.ROOT)
    );
    if (!users.isEmpty()) {
      UUID user = users.get(0);
      String raw = LocalAuthService.secret();
      db.update(
        "insert into account_action_tokens values(?,?,?,?,null)",
        LocalAuthService.hash(raw),
        user,
        purpose,
        java.sql.Timestamp.from(
          java.time.Instant.now()
            .plusSeconds(purpose.equals("RESET") ? 3600 : 86400)
        )
      );
      var message = new SimpleMailMessage();
      message.setFrom(from);
      message.setTo(email);
      message.setSubject(
        purpose.equals("RESET")
          ? "Reset your iStudy password"
          : "Verify your iStudy email"
      );
      message.setText(
        "Open this link to continue: " +
        frontend +
        (purpose.equals("RESET")
            ? "/reset-password?token="
            : "/verify-email?token=") +
        raw
      );
      mail.getObject().send(message);
    }
    return Map.of(
      "message",
      "If the account is eligible, an email has been sent."
    );
  }

  @PostMapping("/password-reset")
  @Transactional
  public Map<String, Object> reset(@Valid @RequestBody Email request) {
    return send(request.email(), "RESET");
  }

  @PostMapping("/password-reset/validate")
  public Map<String, Object> validate(@Valid @RequestBody Token request) {
    var row = token(request.token(), "RESET", false);
    return Map.of("valid", true, "email", row.get("email"));
  }

  @PostMapping("/password-reset/complete")
  @Transactional
  public Map<String, Object> complete(@Valid @RequestBody Password request) {
    var row = token(request.token(), "RESET", true);
    if (
      request
        .password()
        .getBytes(java.nio.charset.StandardCharsets.UTF_8)
        .length >
      72
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Use a password of at most 72 bytes"
    );
    db.update(
      "update local_credentials set password_hash=? where user_id=?",
      new BCryptPasswordEncoder().encode(request.password()),
      row.get("user_id")
    );
    db.update(
      "update account_action_tokens set consumed_at=current_timestamp where token_hash=?",
      LocalAuthService.hash(request.token())
    );
    db.update("delete from auth_sessions where user_id=?", row.get("user_id"));
    return Map.of("message", "Your password has been reset");
  }

  @PostMapping("/email-verification")
  @Transactional
  public Map<String, Object> verifyRequest() {
    return send(
      db.queryForObject(
        "select email from app_users where user_id=?",
        String.class,
        current.id()
      ),
      "VERIFY"
    );
  }

  @PostMapping("/email-verification/complete")
  @Transactional
  public Map<String, Object> verify(@Valid @RequestBody Token request) {
    var matches = db.queryForList(
      "select t.user_id,t.consumed_at,u.email_verified from account_action_tokens t join app_users u on u.user_id=t.user_id where t.token_hash=? and t.purpose='VERIFY' and t.expires_at>current_timestamp and u.status='ACTIVE' for update",
      LocalAuthService.hash(request.token())
    );
    if (matches.isEmpty()) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "This link is invalid or has expired"
    );
    var row = matches.get(0);
    if (
      row.get("consumed_at") != null &&
      Boolean.TRUE.equals(row.get("email_verified"))
    ) return Map.of(
      "message",
      "Your email is verified",
      "alreadyVerified",
      true
    );
    db.update(
      "update app_users set email_verified=true,updated_at=current_timestamp where user_id=?",
      row.get("user_id")
    );
    db.update(
      "update account_action_tokens set consumed_at=current_timestamp where token_hash=?",
      LocalAuthService.hash(request.token())
    );
    return Map.of("message", "Your email is verified");
  }
}
