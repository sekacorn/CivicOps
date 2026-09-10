package org.civicops.volunteers;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.*;
import org.civicops.core.membership.*;
import org.civicops.core.security.*;
import org.civicops.volunteers.security.VolunteerAccessService;
import org.civicops.volunteers.volunteer.VolunteerRepository;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.security.access.AccessDeniedException;

class VolunteerAccessServiceTest {
  @Mock CurrentUserProvider current;
  @Mock OrganizationMembershipRepository memberships;
  @Mock VolunteerRepository volunteers;
  private AutoCloseable mocks;
  private VolunteerAccessService access;
  private UUID userId;
  private UUID orgId;

  @BeforeEach
  void setup() {
    mocks = MockitoAnnotations.openMocks(this);
    userId = UUID.randomUUID();
    orgId = UUID.randomUUID();
    when(current.currentUserId()).thenReturn(userId);
    access =
        new VolunteerAccessService(current, new OrganizationAccessService(memberships), volunteers);
  }

  @AfterEach
  void close() throws Exception {
    mocks.close();
  }

  @Test
  void coordinatorCanManageUpdates() {
    membership(Role.VOLUNTEER_COORDINATOR);
    assertThatCode(() -> access.requireManage(orgId)).doesNotThrowAnyException();
  }

  @Test
  void viewerCannotManageUpdates() {
    membership(Role.VIEWER);
    assertThatThrownBy(() -> access.requireManage(orgId)).isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void programManagerCanManageOpportunitiesButNotPrivateVolunteerProfiles() {
    membership(Role.PROGRAM_MANAGER);
    assertThatCode(() -> access.requireOpportunityManage(orgId)).doesNotThrowAnyException();
    assertThatThrownBy(() -> access.requireManage(orgId)).isInstanceOf(AccessDeniedException.class);
  }

  private void membership(Role role) {
    OrganizationMembership membership = mock(OrganizationMembership.class);
    when(membership.getRole()).thenReturn(role);
    when(memberships.findByOrganizationIdAndUserIdAndActiveTrue(orgId, userId))
        .thenReturn(Optional.of(membership));
  }
}
