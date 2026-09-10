package org.civicops.donations.security;

import java.util.UUID;
import org.civicops.core.membership.Role;
import org.civicops.core.security.*;
import org.springframework.stereotype.Service;

@Service
public class DonationAccessService {
  private final CurrentUserProvider current;
  private final OrganizationAccessService access;

  public DonationAccessService(CurrentUserProvider current, OrganizationAccessService access) {
    this.current = current;
    this.access = access;
  }

  public UUID userId() {
    return current.currentUserId();
  }

  public void requireSummaryRead(UUID org) {
    access.requireAnyRole(userId(), org, Role.DONATION_MANAGER, Role.PROGRAM_MANAGER, Role.VIEWER);
  }

  public void requireManage(UUID org) {
    access.requireAnyRole(userId(), org, Role.DONATION_MANAGER);
  }
}
