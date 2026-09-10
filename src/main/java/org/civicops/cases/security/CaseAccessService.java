package org.civicops.cases.security;

import java.util.UUID;
import org.civicops.cases.casefile.*;
import org.civicops.core.membership.*;
import org.civicops.core.security.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class CaseAccessService {
  private final CurrentUserProvider current;
  private final OrganizationAccessService access;
  private final CaseRecordRepository cases;

  public CaseAccessService(
      CurrentUserProvider c, OrganizationAccessService a, CaseRecordRepository cases) {
    current = c;
    access = a;
    this.cases = cases;
  }

  public UUID userId() {
    return current.currentUserId();
  }

  public void requireCaseManager(UUID org) {
    access.requireAnyRole(userId(), org, Role.CASE_MANAGER);
  }

  public void requireReporting(UUID org) {
    access.requireAnyRole(userId(), org, Role.CASE_MANAGER, Role.PROGRAM_MANAGER);
  }

  public void requireWorkloadReporting(UUID org) {
    requireCaseManager(org);
  }

  public UUID requireListAccessAndWorkerScope(UUID org) {
    OrganizationMembership m =
        access.requireAnyRole(userId(), org, Role.CASE_MANAGER, Role.CASE_WORKER);
    return m.getRole() == Role.CASE_WORKER ? userId() : null;
  }

  public void requireCaseManagementRead(UUID org, CaseRecord record) {
    OrganizationMembership m =
        access.requireAnyRole(userId(), org, Role.CASE_MANAGER, Role.CASE_WORKER);
    if (m.getRole() == Role.CASE_WORKER
        && (record.getAssignedUser() == null || !record.getAssignedUser().getId().equals(userId())))
      deny();
  }

  public void requireAssignedCaseAccess(UUID org, CaseRecord record) {
    requireCaseManagementRead(org, record);
  }

  public void requireSensitiveCaseAccess(UUID org, CaseRecord record) {
    requireCaseManagementRead(org, record);
  }

  public boolean canManageCase(UUID org, CaseRecord record) {
    try {
      requireCaseManagementRead(org, record);
      return true;
    } catch (AccessDeniedException e) {
      return false;
    }
  }

  public boolean canReadClientDetails(UUID org, UUID client) {
    OrganizationMembership m =
        access.requireAnyRole(userId(), org, Role.CASE_MANAGER, Role.CASE_WORKER);
    return m.getRole() != Role.CASE_WORKER
        || cases.existsByOrganizationIdAndClientIdAndAssignedUserId(org, client, userId());
  }

  public void requireClientDetails(UUID org, UUID client) {
    if (!canReadClientDetails(org, client)) deny();
  }

  private static void deny() {
    throw new AccessDeniedException("Case access is limited to the assigned worker");
  }
}
