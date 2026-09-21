package org.civicops.core.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.civicops.core.membership.OrganizationMembershipService;
import org.civicops.core.user.UserService;
import org.junit.jupiter.api.Test;

class AuthenticationControllerTest {
  @Test
  void returnsOnlyTheAuthenticatedUsersActiveMemberships() {
    UUID userId = UUID.randomUUID();
    AuthenticationService authentication = mock(AuthenticationService.class);
    CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
    UserService users = mock(UserService.class);
    OrganizationMembershipService memberships = mock(OrganizationMembershipService.class);
    when(currentUser.currentUserId()).thenReturn(userId);
    when(memberships.listActiveForUser(userId)).thenReturn(List.of());
    AuthenticationController controller =
        new AuthenticationController(authentication, currentUser, users, memberships);

    assertThat(controller.myMemberships()).isEmpty();

    verify(memberships).listActiveForUser(userId);
  }
}
