package org.civicops.core.security;

import java.util.UUID;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {
  public CivicOpsPrincipal currentPrincipal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof CivicOpsPrincipal principal)) {
      throw new AuthenticationCredentialsNotFoundException("Authentication is required");
    }
    return principal;
  }

  public UUID currentUserId() {
    return currentPrincipal().userId();
  }
}
