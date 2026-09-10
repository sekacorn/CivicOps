package org.civicops.grants;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.civicops.core.membership.*;
import org.civicops.core.security.*;
import org.civicops.grants.security.GrantAccessService;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;

class GrantAccessServiceTest {
  CurrentUserProvider current = mock(CurrentUserProvider.class);
  OrganizationMembershipRepository memberships = mock(OrganizationMembershipRepository.class);
  GrantAccessService access =
      new GrantAccessService(current, new OrganizationAccessService(memberships));
  UUID userId = UUID.randomUUID(), orgId = UUID.randomUUID();

  @BeforeEach
  void setup() {
    when(current.currentUserId()).thenReturn(userId);
  }

  @Test
  void grantManagerCanReadAndManage() {
    membership(Role.GRANT_MANAGER);
    assertThatCode(
            () -> {
              access.requireRead(orgId);
              access.requireManage(orgId);
            })
        .doesNotThrowAnyException();
  }

  @Test
  void organizationAdminCanManage() {
    membership(Role.ORG_ADMIN);
    assertThatCode(() -> access.requireManage(orgId)).doesNotThrowAnyException();
  }

  @Test
  void programManagerAndViewerAreReadOnly() {
    membership(Role.PROGRAM_MANAGER);
    assertThatCode(() -> access.requireRead(orgId)).doesNotThrowAnyException();
    assertThatThrownBy(() -> access.requireManage(orgId)).isInstanceOf(AccessDeniedException.class);
    reset(memberships);
    membership(Role.VIEWER);
    assertThatCode(() -> access.requireRead(orgId)).doesNotThrowAnyException();
    assertThatThrownBy(() -> access.requireManage(orgId)).isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void unrelatedRolesCannotMutateGrants() {
    for (Role role :
        List.of(
            Role.VOLUNTEER_COORDINATOR,
            Role.VOLUNTEER,
            Role.EVENT_COORDINATOR,
            Role.DONATION_MANAGER)) {
      reset(memberships);
      membership(role);
      assertThatThrownBy(() -> access.requireManage(orgId))
          .isInstanceOf(AccessDeniedException.class);
    }
  }

  private void membership(Role role) {
    OrganizationMembership m = mock(OrganizationMembership.class);
    when(m.getRole()).thenReturn(role);
    when(memberships.findByOrganizationIdAndUserIdAndActiveTrue(orgId, userId))
        .thenReturn(Optional.of(m));
  }
}
