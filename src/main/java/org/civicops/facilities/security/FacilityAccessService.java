package org.civicops.facilities.security;

import java.util.UUID;
import org.civicops.core.membership.Role;
import org.civicops.core.security.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class FacilityAccessService {
  private final CurrentUserProvider current;
  private final OrganizationAccessService access;

  public FacilityAccessService(CurrentUserProvider c, OrganizationAccessService a) {
    current = c;
    access = a;
  }

  public UUID userId() {
    return current.currentUserId();
  }

  public void requireFacilityManagement(UUID org) {
    access.requireAnyRole(userId(), org, Role.FACILITY_MANAGER);
  }

  public void requireReservationApproval(UUID org) {
    requireFacilityManagement(org);
  }

  public void requireFacilityRead(UUID org) {
    access.requireAnyRole(
        userId(),
        org,
        Role.FACILITY_MANAGER,
        Role.EVENT_COORDINATOR,
        Role.PROGRAM_MANAGER,
        Role.VIEWER);
  }

  public void requireReservationRead(UUID org) {
    access.requireAnyRole(
        userId(),
        org,
        Role.FACILITY_MANAGER,
        Role.EVENT_COORDINATOR,
        Role.PROGRAM_MANAGER,
        Role.VIEWER);
  }

  public void requireReservationRequest(UUID org) {
    access.requireAnyRole(
        userId(), org, Role.FACILITY_MANAGER, Role.EVENT_COORDINATOR, Role.PROGRAM_MANAGER);
  }

  public boolean isManager(UUID org) {
    try {
      requireFacilityManagement(org);
      return true;
    } catch (AccessDeniedException e) {
      return false;
    }
  }

  public void requireOwnReservationAccess(UUID org, UUID requester) {
    access.requireMembership(userId(), org);
    if (!userId().equals(requester) && !isManager(org))
      throw new AccessDeniedException(
          "Reservation private detail is available only to its requester or facility management");
  }
}
