package com.istudy.identity;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentStudent {

  public UUID id() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (
      auth == null ||
      !auth.isAuthenticated() ||
      !(auth.getPrincipal() instanceof UUID)
    ) throw new ResponseStatusException(
      HttpStatus.UNAUTHORIZED,
      "Sign in to continue"
    );
    return (UUID) auth.getPrincipal();
  }
}
