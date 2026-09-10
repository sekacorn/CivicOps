package org.civicops.core.organization;

import java.util.UUID;
import org.civicops.core.membership.OrganizationMembershipService;
import org.civicops.core.membership.Role;
import org.civicops.core.membership.dto.CreateMembershipRequest;
import org.civicops.core.organization.dto.CreateOrganizationRequest;
import org.civicops.core.organization.dto.OrganizationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationProvisioningService {
  private final OrganizationService organizations;
  private final OrganizationMembershipService memberships;

  public OrganizationProvisioningService(
      OrganizationService organizations, OrganizationMembershipService memberships) {
    this.organizations = organizations;
    this.memberships = memberships;
  }

  @Transactional
  public OrganizationResponse createForUser(UUID userId, CreateOrganizationRequest request) {
    OrganizationResponse organization = organizations.create(request);
    memberships.create(organization.id(), new CreateMembershipRequest(userId, Role.ORG_ADMIN));
    return organization;
  }
}
