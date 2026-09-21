package org.civicops.core.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.civicops.core.membership.dto.CreateMembershipRequest;
import org.civicops.core.organization.Organization;
import org.civicops.core.organization.OrganizationService;
import org.civicops.core.user.User;
import org.civicops.core.user.UserService;
import org.civicops.shared.exception.BusinessRuleException;
import org.civicops.shared.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationMembershipServiceTest {
  @Mock OrganizationMembershipRepository memberships;
  @Mock OrganizationService organizations;
  @Mock UserService users;
  private OrganizationMembershipService service;
  private final UUID organizationId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service =
        new OrganizationMembershipService(
            memberships,
            organizations,
            users,
            Clock.fixed(Instant.parse("2026-08-29T12:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void createsMembershipForActiveUserAndOrganization() {
    Organization organization = mock(Organization.class);
    User user = mock(User.class);
    when(organization.isActive()).thenReturn(true);
    when(user.isActive()).thenReturn(true);
    when(organization.getId()).thenReturn(organizationId);
    when(user.getId()).thenReturn(userId);
    when(organizations.requireEntity(organizationId)).thenReturn(organization);
    when(users.requireEntity(userId)).thenReturn(user);
    when(memberships.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var result = service.create(organizationId, new CreateMembershipRequest(userId, Role.VIEWER));

    assertThat(result.organizationId()).isEqualTo(organizationId);
    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.role()).isEqualTo(Role.VIEWER);
    assertThat(result.joinedAt()).isEqualTo(Instant.parse("2026-08-29T12:00:00Z"));
  }

  @Test
  void rejectsDuplicateMembership() {
    when(memberships.existsByOrganizationIdAndUserId(organizationId, userId)).thenReturn(true);

    assertThatThrownBy(
            () -> service.create(organizationId, new CreateMembershipRequest(userId, Role.VIEWER)))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("already has a membership");
  }

  @Test
  void rejectsPlatformRoleAsOrganizationMembership() {
    assertThatThrownBy(
            () ->
                service.create(
                    organizationId, new CreateMembershipRequest(userId, Role.SYSTEM_ADMIN)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("platform role");
  }

  @Test
  void rejectsInactiveMembershipParty() {
    Organization organization = mock(Organization.class);
    User user = mock(User.class);
    when(organization.isActive()).thenReturn(false);
    when(organizations.requireEntity(organizationId)).thenReturn(organization);
    when(users.requireEntity(userId)).thenReturn(user);

    assertThatThrownBy(
            () -> service.create(organizationId, new CreateMembershipRequest(userId, Role.VIEWER)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("active organization and user");
  }

  @Test
  void listsOnlyActiveMembershipsForCurrentUserContext() {
    when(memberships.findActiveOrganizationsForUser(userId)).thenReturn(List.of());

    assertThat(service.listActiveForUser(userId)).isEmpty();

    verify(users).requireEntity(userId);
    verify(memberships).findActiveOrganizationsForUser(userId);
  }
}
