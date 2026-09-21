package org.civicops.core.membership;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.civicops.core.membership.dto.CreateMembershipRequest;
import org.civicops.core.membership.dto.MembershipResponse;
import org.civicops.core.organization.Organization;
import org.civicops.core.organization.OrganizationService;
import org.civicops.core.user.User;
import org.civicops.core.user.UserService;
import org.civicops.shared.exception.BusinessRuleException;
import org.civicops.shared.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationMembershipService {
  private final OrganizationMembershipRepository memberships;
  private final OrganizationService organizations;
  private final UserService users;
  private final Clock clock;

  public OrganizationMembershipService(
      OrganizationMembershipRepository memberships,
      OrganizationService organizations,
      UserService users,
      Clock clock) {
    this.memberships = memberships;
    this.organizations = organizations;
    this.users = users;
    this.clock = clock;
  }

  @Transactional
  public MembershipResponse create(UUID organizationId, CreateMembershipRequest request) {
    if (!request.role().isOrganizationAssignable()) {
      throw new BusinessRuleException(
          "INVALID_ORGANIZATION_ROLE",
          "SYSTEM_ADMIN is a platform role and cannot be assigned through an organization membership");
    }
    if (memberships.existsByOrganizationIdAndUserId(organizationId, request.userId())) {
      throw new ConflictException(
          "DUPLICATE_MEMBERSHIP", "The user already has a membership in this organization");
    }
    Organization organization = organizations.requireEntity(organizationId);
    User user = users.requireEntity(request.userId());
    if (!organization.isActive() || !user.isActive()) {
      throw new BusinessRuleException(
          "INACTIVE_MEMBERSHIP_PARTY", "Memberships require an active organization and user");
    }
    OrganizationMembership membership =
        new OrganizationMembership(organization, user, request.role(), Instant.now(clock));
    return MembershipResponse.from(memberships.save(membership));
  }

  @Transactional(readOnly = true)
  public List<MembershipResponse> listForOrganization(UUID organizationId) {
    organizations.requireEntity(organizationId);
    return memberships.findAllByOrganizationId(organizationId).stream()
        .map(MembershipResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<MembershipResponse> listActiveForUser(UUID userId) {
    users.requireEntity(userId);
    return memberships.findActiveOrganizationsForUser(userId).stream()
        .map(MembershipResponse::from)
        .toList();
  }
}
