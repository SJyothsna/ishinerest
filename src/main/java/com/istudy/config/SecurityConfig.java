package com.istudy.config;

import com.istudy.identity.LocalAuthService;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain api(
    HttpSecurity http,
    LocalAuthService auth,
    CorsConfigurationSource cors
  ) throws Exception {
    var bearer = new OncePerRequestFilter() {
      protected void doFilterInternal(
        jakarta.servlet.http.HttpServletRequest request,
        jakarta.servlet.http.HttpServletResponse response,
        jakarta.servlet.FilterChain chain
      ) throws jakarta.servlet.ServletException, java.io.IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) auth
          .authenticate(header.substring(7))
          .ifPresent(id ->
            SecurityContextHolder.getContext()
              .setAuthentication(
                new UsernamePasswordAuthenticationToken(id, null, List.of())
              )
          );
        chain.doFilter(request, response);
      }
    };
    return http
      .cors(c -> c.configurationSource(cors))
      .csrf(c -> c.disable())
      .sessionManagement(c ->
        c.sessionCreationPolicy(
          org.springframework.security.config.http.SessionCreationPolicy.STATELESS
        )
      )
      .authorizeHttpRequests(a ->
        a
          .requestMatchers(
            "/api/v1/catalogue/**",
            "/api/v1/guest-practice/**",
            "/api/v1/feedback",
            "/api/v1/auth/login",
            "/api/v1/auth/signup",
            "/api/v1/auth/config",
            "/api/v1/auth/password-reset",
            "/api/v1/auth/password-reset/**",
            "/api/v1/auth/email-verification/complete",
            "/actuator/health"
          )
          .permitAll()
          .anyRequest()
          .authenticated()
      )
      .exceptionHandling(e ->
        e.authenticationEntryPoint((req, res, error) -> res.sendError(401))
      )
      .addFilterBefore(bearer, UsernamePasswordAuthenticationFilter.class)
      .build();
  }

  @Bean
  CorsConfigurationSource cors(@Value("${app.frontend-url}") String origin) {
    var c = new CorsConfiguration();
    c.setAllowedOrigins(List.of(origin));
    c.setAllowCredentials(true);
    c.setAllowedMethods(
      List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
    );
    c.setAllowedHeaders(
      List.of("Authorization", "Content-Type", "Idempotency-Key")
    );
    var s = new UrlBasedCorsConfigurationSource();
    s.registerCorsConfiguration("/**", c);
    return s;
  }
}
