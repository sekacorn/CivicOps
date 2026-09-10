package org.civicops.events.security;

import java.util.*;
import org.civicops.core.membership.Role;
import org.civicops.core.security.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class EventAccessService {
  private final CurrentUserProvider current;
  private final OrganizationAccessService access;

  public EventAccessService(CurrentUserProvider c, OrganizationAccessService a) {
    current = c;
    access = a;
  }

  public UUID userId() {
    return current.currentUserId();
  }

  public void requireRead(UUID org) {
    access.requireAnyRole(
        userId(),
        org,
        Role.EVENT_COORDINATOR,
        Role.PROGRAM_MANAGER,
        Role.VOLUNTEER_COORDINATOR,
        Role.GRANT_MANAGER,
        Role.DONATION_MANAGER,
        Role.VIEWER,
        Role.VOLUNTEER);
  }

  public void requireManage(UUID org) {
    access.requireAnyRole(userId(), org, Role.EVENT_COORDINATOR);
  }

  public void requireRegistrationRead(UUID org) {
    access.requireAnyRole(userId(), org, Role.EVENT_COORDINATOR, Role.PROGRAM_MANAGER);
  }

  public void requireSelfService(UUID org) {
    access.requireMembership(userId(), org);
  }

  public void requireOwnOrManage(UUID org, UUID registrationUser) {
    try {
      access.requireAnyRole(userId(), org, Role.EVENT_COORDINATOR);
      return;
    } catch (AccessDeniedException ignored) {
    }
    if (!userId().equals(registrationUser))
      throw new AccessDeniedException("Registration self-service is limited to the current user");
  }
}
