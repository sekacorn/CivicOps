package org.civicops.grants.security;

import java.util.UUID;
import org.civicops.core.membership.Role;
import org.civicops.core.security.CurrentUserProvider;
import org.civicops.core.security.OrganizationAccessService;
import org.springframework.stereotype.Service;

@Service
public class GrantAccessService {
  private final CurrentUserProvider current;
  private final OrganizationAccessService organizations;

  public GrantAccessService(CurrentUserProvider current, OrganizationAccessService organizations) {
    this.current = current;
    this.organizations = organizations;
  }

  public UUID userId() {
    return current.currentUserId();
  }

  public void requireRead(UUID organizationId) {
    organizations.requireAnyRole(
        userId(), organizationId, Role.GRANT_MANAGER, Role.PROGRAM_MANAGER, Role.VIEWER);
  }

  public void requireManage(UUID organizationId) {
    organizations.requireAnyRole(userId(), organizationId, Role.GRANT_MANAGER);
  }
}
