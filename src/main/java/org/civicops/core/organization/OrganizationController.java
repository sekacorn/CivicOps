package org.civicops.core.organization;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.civicops.core.organization.dto.CreateOrganizationRequest;
import org.civicops.core.organization.dto.OrganizationResponse;
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
@RequestMapping("/api/v1/organizations")
public class OrganizationController {
  private final OrganizationService organizations;
  private final OrganizationProvisioningService provisioning;
  private final OrganizationAccessService access;
  private final CurrentUserProvider currentUser;

  public OrganizationController(
      OrganizationService organizations,
      OrganizationProvisioningService provisioning,
      OrganizationAccessService access,
      CurrentUserProvider currentUser) {
    this.organizations = organizations;
    this.provisioning = provisioning;
    this.access = access;
    this.currentUser = currentUser;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OrganizationResponse create(@Valid @RequestBody CreateOrganizationRequest request) {
    return provisioning.createForUser(currentUser.currentUserId(), request);
  }

  @GetMapping("/{organizationId}")
  public OrganizationResponse get(@PathVariable UUID organizationId) {
    access.requireMembership(currentUser.currentUserId(), organizationId);
    return organizations.get(organizationId);
  }

  @GetMapping
  public List<OrganizationResponse> list() {
    return access.organizationsForUser(currentUser.currentUserId()).stream()
        .map(OrganizationResponse::from)
        .toList();
  }
}
