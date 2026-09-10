package org.civicops.grantreporting.security;

import java.util.UUID;
import org.civicops.core.membership.Role;
import org.civicops.core.security.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class GrantReportingAccessService {
  private final CurrentUserProvider current;
  private final OrganizationAccessService organizations;

  public GrantReportingAccessService(CurrentUserProvider c, OrganizationAccessService o) {
    current = c;
    organizations = o;
  }

  public UUID userId() {
    return current.currentUserId();
  }

  public void requireReportManagement(UUID org) {
    organizations.requireAnyRole(userId(), org, Role.GRANT_MANAGER);
  }

  public void requireTemplateManagement(UUID org) {
    requireReportManagement(org);
  }

  public void requireFinalize(UUID org) {
    requireReportManagement(org);
  }

  public void requireReportRead(UUID org) {
    organizations.requireAnyRole(userId(), org, Role.GRANT_MANAGER, Role.PROGRAM_MANAGER);
  }

  public void requireEvidenceRead(UUID org) {
    requireReportRead(org);
  }

  public boolean canManage(UUID org) {
    try {
      requireReportManagement(org);
      return true;
    } catch (AccessDeniedException e) {
      return false;
    }
  }
}
