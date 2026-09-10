package org.civicops.grantreporting;

import static org.mockito.Mockito.*;

import java.util.UUID;
import org.civicops.core.membership.Role;
import org.civicops.core.security.CurrentUserProvider;
import org.civicops.core.security.OrganizationAccessService;
import org.civicops.grantreporting.security.GrantReportingAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class GrantReportingAccessServiceTest {
  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final OrganizationAccessService organizations = mock(OrganizationAccessService.class);
  private final GrantReportingAccessService access =
      new GrantReportingAccessService(current, organizations);
  private final UUID userId = UUID.randomUUID();
  private final UUID organizationId = UUID.randomUUID();

  GrantReportingAccessServiceTest() {
    when(current.currentUserId()).thenReturn(userId);
  }

  @Test
  void managementUsesGrantManagerBoundary() {
    access.requireReportManagement(organizationId);
    verify(organizations).requireAnyRole(userId, organizationId, Role.GRANT_MANAGER);
  }

  @Test
  void templatesAndFinalizationUseSameManagementBoundary() {
    access.requireTemplateManagement(organizationId);
    access.requireFinalize(organizationId);
    verify(organizations, times(2)).requireAnyRole(userId, organizationId, Role.GRANT_MANAGER);
  }

  @Test
  void finalizedReadPolicyIncludesProgramManager() {
    access.requireReportRead(organizationId);
    access.requireEvidenceRead(organizationId);
    verify(organizations, times(2))
        .requireAnyRole(userId, organizationId, Role.GRANT_MANAGER, Role.PROGRAM_MANAGER);
  }

  @Test
  void canManageReturnsFalseWhenRoleBoundaryRejectsUser() {
    doThrow(new AccessDeniedException("denied"))
        .when(organizations)
        .requireAnyRole(userId, organizationId, Role.GRANT_MANAGER);
    org.assertj.core.api.Assertions.assertThat(access.canManage(organizationId)).isFalse();
  }
}
