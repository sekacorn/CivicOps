package org.civicops.facilities;

import static org.mockito.Mockito.*;

import java.util.UUID;
import org.civicops.core.membership.Role;
import org.civicops.core.security.*;
import org.civicops.facilities.security.FacilityAccessService;
import org.junit.jupiter.api.Test;

class FacilityAccessServiceTest {
  CurrentUserProvider current = mock(CurrentUserProvider.class);
  OrganizationAccessService orgs = mock(OrganizationAccessService.class);
  FacilityAccessService access = new FacilityAccessService(current, orgs);
  UUID user = UUID.randomUUID(), org = UUID.randomUUID();

  FacilityAccessServiceTest() {
    when(current.currentUserId()).thenReturn(user);
  }

  @Test
  void managementUsesFacilityManagerBoundary() {
    access.requireFacilityManagement(org);
    verify(orgs).requireAnyRole(user, org, Role.FACILITY_MANAGER);
  }

  @Test
  void readsUseLeastPrivilegeSet() {
    access.requireFacilityRead(org);
    verify(orgs)
        .requireAnyRole(
            user,
            org,
            Role.FACILITY_MANAGER,
            Role.EVENT_COORDINATOR,
            Role.PROGRAM_MANAGER,
            Role.VIEWER);
  }

  @Test
  void requestsAllowOperationalRoles() {
    access.requireReservationRequest(org);
    verify(orgs)
        .requireAnyRole(
            user, org, Role.FACILITY_MANAGER, Role.EVENT_COORDINATOR, Role.PROGRAM_MANAGER);
  }
}
