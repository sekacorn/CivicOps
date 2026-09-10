package org.civicops.donations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.civicops.core.membership.*;
import org.civicops.core.security.*;
import org.civicops.donations.security.DonationAccessService;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;

class DonationAccessServiceTest {
  CurrentUserProvider current = mock(CurrentUserProvider.class);
  OrganizationMembershipRepository memberships = mock(OrganizationMembershipRepository.class);
  DonationAccessService access =
      new DonationAccessService(current, new OrganizationAccessService(memberships));
  UUID user = UUID.randomUUID(), org = UUID.randomUUID();

  @BeforeEach
  void setup() {
    when(current.currentUserId()).thenReturn(user);
  }

  @Test
  void donationManagerCanReadAndManage() {
    membership(Role.DONATION_MANAGER);
    assertThatCode(
            () -> {
              access.requireSummaryRead(org);
              access.requireManage(org);
            })
        .doesNotThrowAnyException();
  }

  @Test
  void orgAdminCanManage() {
    membership(Role.ORG_ADMIN);
    assertThatCode(() -> access.requireManage(org)).doesNotThrowAnyException();
  }

  @Test
  void programManagerAndViewerAreReadOnly() {
    membership(Role.PROGRAM_MANAGER);
    assertThatCode(() -> access.requireSummaryRead(org)).doesNotThrowAnyException();
    assertThatThrownBy(() -> access.requireManage(org)).isInstanceOf(AccessDeniedException.class);
    reset(memberships);
    membership(Role.VIEWER);
    assertThatCode(() -> access.requireSummaryRead(org)).doesNotThrowAnyException();
  }

  @Test
  void unrelatedRolesCannotManage() {
    for (Role role :
        List.of(
            Role.GRANT_MANAGER,
            Role.VOLUNTEER_COORDINATOR,
            Role.EVENT_COORDINATOR,
            Role.VOLUNTEER)) {
      reset(memberships);
      membership(role);
      assertThatThrownBy(() -> access.requireManage(org)).isInstanceOf(AccessDeniedException.class);
    }
  }

  private void membership(Role role) {
    OrganizationMembership m = mock(OrganizationMembership.class);
    when(m.getRole()).thenReturn(role);
    when(memberships.findByOrganizationIdAndUserIdAndActiveTrue(org, user))
        .thenReturn(Optional.of(m));
  }
}
