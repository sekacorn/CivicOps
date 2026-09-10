package org.civicops.foodpantry;

import static org.mockito.Mockito.*;

import java.util.*;
import org.civicops.core.membership.Role;
import org.civicops.core.security.*;
import org.civicops.foodpantry.security.FoodPantryAccessService;
import org.junit.jupiter.api.Test;

class FoodPantryAccessServiceTest {
  CurrentUserProvider current = mock(CurrentUserProvider.class);
  OrganizationAccessService organizations = mock(OrganizationAccessService.class);
  FoodPantryAccessService access = new FoodPantryAccessService(current, organizations);
  UUID user = UUID.randomUUID(), org = UUID.randomUUID();

  FoodPantryAccessServiceTest() {
    when(current.currentUserId()).thenReturn(user);
  }

  @Test
  void managementUsesPantryManagerBoundary() {
    access.requirePantryManagement(org);
    verify(organizations).requireAnyRole(user, org, Role.FOOD_PANTRY_MANAGER);
  }

  @Test
  void distributionUsesManagementBoundary() {
    access.requireDistributionManagement(org);
    verify(organizations).requireAnyRole(user, org, Role.FOOD_PANTRY_MANAGER);
  }

  @Test
  void householdDetailUsesManagementBoundary() {
    access.requireHouseholdDetail(org);
    verify(organizations).requireAnyRole(user, org, Role.FOOD_PANTRY_MANAGER);
  }

  @Test
  void inventoryReadAllowsPrivacySafeRoles() {
    access.requireInventoryRead(org);
    verify(organizations)
        .requireAnyRole(user, org, Role.FOOD_PANTRY_MANAGER, Role.PROGRAM_MANAGER, Role.VIEWER);
  }

  @Test
  void reportingAllowsProgramManager() {
    access.requirePantryReporting(org);
    verify(organizations).requireAnyRole(user, org, Role.FOOD_PANTRY_MANAGER, Role.PROGRAM_MANAGER);
  }
}
