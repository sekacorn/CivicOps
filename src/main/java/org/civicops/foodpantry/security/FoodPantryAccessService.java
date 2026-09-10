package org.civicops.foodpantry.security;

import java.util.UUID;
import org.civicops.core.membership.Role;
import org.civicops.core.security.*;
import org.springframework.stereotype.Service;

@Service
public class FoodPantryAccessService {
  private final CurrentUserProvider current;
  private final OrganizationAccessService organizations;

  public FoodPantryAccessService(CurrentUserProvider c, OrganizationAccessService o) {
    current = c;
    organizations = o;
  }

  public UUID userId() {
    return current.currentUserId();
  }

  public void requirePantryManagement(UUID org) {
    organizations.requireAnyRole(userId(), org, Role.FOOD_PANTRY_MANAGER);
  }

  public void requireInventoryRead(UUID org) {
    organizations.requireAnyRole(
        userId(), org, Role.FOOD_PANTRY_MANAGER, Role.PROGRAM_MANAGER, Role.VIEWER);
  }

  public void requireHouseholdDetail(UUID org) {
    requirePantryManagement(org);
  }

  public void requireDistributionManagement(UUID org) {
    requirePantryManagement(org);
  }

  public void requirePantryReporting(UUID org) {
    organizations.requireAnyRole(userId(), org, Role.FOOD_PANTRY_MANAGER, Role.PROGRAM_MANAGER);
  }
}
