package com.istudy.identity;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LocalAuthService {

  private final JdbcTemplate db;
  private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();

  public LocalAuthService(JdbcTemplate db) {
    this.db = db;
  }

  public static String hash(String secret) {
    try {
      return HexFormat.of()
        .formatHex(
          MessageDigest.getInstance("SHA-256").digest(
            secret.getBytes(StandardCharsets.UTF_8)
          )
        );
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public static String secret() {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  @Transactional
  public Map<String, Object> signup(
    String name,
    String email,
    String password,
    String role
  ) {
    email = email.trim().toLowerCase(Locale.ROOT);
    if (
      !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") ||
      password.length() < 8 ||
      password.getBytes(StandardCharsets.UTF_8).length > 72 ||
      name.isBlank()
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Provide your name, a valid email and a password with at least 8 characters"
    );
    if (
      !Set.of("STUDENT", "PARENT").contains(role)
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "This account type is not available yet"
    );
    if (
      Boolean.TRUE.equals(
        db.queryForObject(
          "select exists(select 1 from app_users where lower(email)=? and status<>'DELETED')",
          Boolean.class,
          email
        )
      )
    ) throw new ResponseStatusException(
      HttpStatus.CONFLICT,
      "An account already uses this email"
    );
    UUID id = UUID.randomUUID();
    db.update(
      "insert into app_users(user_id,email,display_name,account_role) values(?,?,?,?)",
      id,
      email,
      name,
      role
    );
    db.update(
      "insert into local_credentials(user_id,password_hash) values(?,?)",
      id,
      passwords.encode(password)
    );
    db.update(
      "insert into auth_identities(user_id,provider,provider_subject) values(?,'LOCAL',?)",
      id,
      id.toString()
    );
    if (role.equals("STUDENT")) db.update(
      "insert into student_profiles(student_id) values(?)",
      id
    );
    return login(email, password);
  }

  @Transactional
  public Map<String, Object> login(String email, String password) {
    var rows = db.queryForList(
      "select u.*,c.password_hash from app_users u join local_credentials c on c.user_id=u.user_id where lower(u.email)=? and u.status='ACTIVE'",
      email.trim().toLowerCase(Locale.ROOT)
    );
    if (
      rows.isEmpty() ||
      !passwords.matches(password, rows.get(0).get("password_hash").toString())
    ) throw new ResponseStatusException(
      HttpStatus.UNAUTHORIZED,
      "Invalid email or password"
    );
    UUID id = (UUID) rows.get(0).get("user_id");
    String token = secret();
    db.update(
      "insert into auth_sessions(token_hash,user_id,expires_at) values(?,?,?)",
      hash(token),
      id,
      java.sql.Timestamp.from(java.time.Instant.now().plusSeconds(43200))
    );
    var result = new LinkedHashMap<>(profile(id));
    result.put("token", token);
    return result;
  }

  public Optional<UUID> authenticate(String token) {
    return db
      .queryForList(
        "select s.user_id from auth_sessions s join app_users u on u.user_id=s.user_id where s.token_hash=? and s.expires_at>current_timestamp and u.status='ACTIVE'",
        UUID.class,
        hash(token)
      )
      .stream()
      .findFirst();
  }

  public Map<String, Object> profile(UUID id) {
    var row = db.queryForMap("select * from app_users where user_id=?", id);
    var result = new LinkedHashMap<String, Object>();
    result.put("userId", id);
    result.put(
      "studentId",
      "STUDENT".equals(row.get("account_role")) ? id : null
    );
    result.put("name", row.get("display_name"));
    result.put("email", row.get("email"));
    result.put("emailVerified", row.get("email_verified"));
    result.put("role", row.get("account_role"));
    result.put("contentAdmin", row.get("content_admin"));
    return result;
  }

  public void logout(String token) {
    db.update("delete from auth_sessions where token_hash=?", hash(token));
  }
}
