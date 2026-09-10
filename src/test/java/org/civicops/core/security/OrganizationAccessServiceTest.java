package org.civicops.core.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.civicops.core.membership.OrganizationMembership;
import org.civicops.core.membership.OrganizationMembershipRepository;
import org.civicops.core.membership.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class OrganizationAccessServiceTest {
  @Mock OrganizationMembershipRepository memberships;

  @Test
  void permitsRequiredOrganizationRole() {
    UUID organizationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    OrganizationMembership membership = mock(OrganizationMembership.class);
    when(membership.getRole()).thenReturn(Role.GRANT_MANAGER);
    when(memberships.findByOrganizationIdAndUserIdAndActiveTrue(organizationId, userId))
        .thenReturn(Optional.of(membership));

    assertThat(
            new OrganizationAccessService(memberships)
                .requireAnyRole(userId, organizationId, Set.of(Role.GRANT_MANAGER)))
        .isSameAs(membership);
  }

  @Test
  void deniesMissingMembership() {
    UUID organizationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(memberships.findByOrganizationIdAndUserIdAndActiveTrue(organizationId, userId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                new OrganizationAccessService(memberships)
                    .requireAnyRole(userId, organizationId, Set.of(Role.VIEWER)))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void organizationAdminSatisfiesAnyOrganizationRole() {
    UUID organizationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    OrganizationMembership membership = mock(OrganizationMembership.class);
    when(membership.getRole()).thenReturn(Role.ORG_ADMIN);
    when(memberships.findByOrganizationIdAndUserIdAndActiveTrue(organizationId, userId))
        .thenReturn(Optional.of(membership));

    assertThat(
            new OrganizationAccessService(memberships)
                .requireAnyRole(userId, organizationId, Set.of(Role.VOLUNTEER_COORDINATOR)))
        .isSameAs(membership);
  }

  @Test
  void viewerCannotAdministerOrganization() {
    UUID organizationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    OrganizationMembership membership = mock(OrganizationMembership.class);
    when(membership.getRole()).thenReturn(Role.VIEWER);
    when(memberships.findByOrganizationIdAndUserIdAndActiveTrue(organizationId, userId))
        .thenReturn(Optional.of(membership));

    assertThatThrownBy(
            () ->
                new OrganizationAccessService(memberships)
                    .requireRole(userId, organizationId, Role.ORG_ADMIN))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void grantManagerCannotAdministerMemberships() {
    UUID organizationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    OrganizationMembership membership = mock(OrganizationMembership.class);
    when(membership.getRole()).thenReturn(Role.GRANT_MANAGER);
    when(memberships.findByOrganizationIdAndUserIdAndActiveTrue(organizationId, userId))
        .thenReturn(Optional.of(membership));

    assertThatThrownBy(
            () ->
                new OrganizationAccessService(memberships)
                    .requireRole(userId, organizationId, Role.ORG_ADMIN))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void inactiveMembershipGrantsNoAccess() {
    UUID organizationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(memberships.findByOrganizationIdAndUserIdAndActiveTrue(organizationId, userId))
        .thenReturn(Optional.empty());

    assertThat(new OrganizationAccessService(memberships).hasMembership(userId, organizationId))
        .isFalse();
  }
}
