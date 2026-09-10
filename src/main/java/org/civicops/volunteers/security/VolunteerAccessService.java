package org.civicops.volunteers.security;

import java.util.UUID;
import org.civicops.core.membership.*;
import org.civicops.core.security.*;
import org.civicops.volunteers.volunteer.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class VolunteerAccessService {
  private final CurrentUserProvider current;
  private final OrganizationAccessService access;
  private final VolunteerRepository volunteers;

  public VolunteerAccessService(
      CurrentUserProvider c, OrganizationAccessService a, VolunteerRepository v) {
    current = c;
    access = a;
    volunteers = v;
  }

  public UUID userId() {
    return current.currentUserId();
  }

  public void requireRead(UUID orgId) {
    access.requireAnyRole(
        userId(), orgId, Role.VOLUNTEER_COORDINATOR, Role.PROGRAM_MANAGER, Role.VIEWER);
  }

  public void requireActivityRead(UUID orgId) {
    access.requireAnyRole(
        userId(),
        orgId,
        Role.VOLUNTEER_COORDINATOR,
        Role.PROGRAM_MANAGER,
        Role.VIEWER,
        Role.VOLUNTEER);
  }

  public void requireManage(UUID orgId) {
    access.requireAnyRole(userId(), orgId, Role.VOLUNTEER_COORDINATOR);
  }

  public void requireOpportunityManage(UUID orgId) {
    access.requireAnyRole(userId(), orgId, Role.VOLUNTEER_COORDINATOR, Role.PROGRAM_MANAGER);
  }

  public Volunteer requireOwnVolunteer(UUID orgId) {
    OrganizationMembership m = access.requireMembership(userId(), orgId);
    if (m.getRole() != Role.VOLUNTEER)
      throw new AccessDeniedException(
          "A volunteer membership is required for this self-service route");
    return volunteers
        .findByOrganizationIdAndUserId(orgId, userId())
        .orElseThrow(() -> new AccessDeniedException("No linked volunteer profile exists"));
  }

  public void requireOwnOrManage(UUID orgId, UUID volunteerId) {
    OrganizationMembership m = access.requireMembership(userId(), orgId);
    if (m.getRole().grantsAny(java.util.Set.of(Role.VOLUNTEER_COORDINATOR))) return;
    Volunteer own = requireOwnVolunteer(orgId);
    if (!own.getId().equals(volunteerId))
      throw new AccessDeniedException("Volunteer self-service is limited to the linked profile");
  }
}
