package org.civicops.equipment;

import static org.mockito.Mockito.*;

import java.util.UUID;
import org.civicops.core.membership.Role;
import org.civicops.core.security.*;
import org.civicops.equipment.security.EquipmentAccessService;
import org.junit.jupiter.api.Test;

class EquipmentAccessServiceTest {
  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final OrganizationAccessService orgs = mock(OrganizationAccessService.class);
  private final EquipmentAccessService access = new EquipmentAccessService(current, orgs);
  private final UUID user = UUID.randomUUID(), org = UUID.randomUUID();

  @Test
  void managerBoundaryUsesEquipmentRole() {
    when(current.currentUserId()).thenReturn(user);
    access.requireManageEquipment(org);
    verify(orgs).requireAnyRole(user, org, Role.EQUIPMENT_MANAGER);
  }

  @Test
  void readBoundaryIncludesOperationalReadRoles() {
    when(current.currentUserId()).thenReturn(user);
    access.requireReadEquipment(org);
    verify(orgs)
        .requireAnyRole(
            user,
            org,
            Role.EQUIPMENT_MANAGER,
            Role.PROGRAM_MANAGER,
            Role.EVENT_COORDINATOR,
            Role.VOLUNTEER_COORDINATOR,
            Role.VIEWER);
  }

  @Test
  void checkoutAndMaintenanceDelegateToLeastPrivilegeManagerBoundary() {
    when(current.currentUserId()).thenReturn(user);
    access.requireCheckoutManagement(org);
    access.requireMaintenanceManagement(org);
    verify(orgs, times(2)).requireAnyRole(user, org, Role.EQUIPMENT_MANAGER);
  }
}
