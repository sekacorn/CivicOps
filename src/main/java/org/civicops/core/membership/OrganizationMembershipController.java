package org.civicops.core.membership;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.civicops.core.membership.dto.CreateMembershipRequest;
import org.civicops.core.membership.dto.MembershipResponse;
import org.civicops.core.security.CurrentUserProvider;
import org.civicops.core.security.OrganizationAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/memberships")
public class OrganizationMembershipController {
  private final OrganizationMembershipService memberships;
  private final OrganizationAccessService access;
  private final CurrentUserProvider currentUser;

  public OrganizationMembershipController(
      OrganizationMembershipService memberships,
      OrganizationAccessService access,
      CurrentUserProvider currentUser) {
    this.memberships = memberships;
    this.access = access;
    this.currentUser = currentUser;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public MembershipResponse create(
      @PathVariable UUID organizationId, @Valid @RequestBody CreateMembershipRequest request) {
    access.requireRole(currentUser.currentUserId(), organizationId, Role.ORG_ADMIN);
    return memberships.create(organizationId, request);
  }

  @GetMapping
  public List<MembershipResponse> list(@PathVariable UUID organizationId) {
    access.requireRole(currentUser.currentUserId(), organizationId, Role.ORG_ADMIN);
    return memberships.listForOrganization(organizationId);
  }
}
