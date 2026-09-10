package org.civicops.equipment.security;

import java.util.UUID;
import org.civicops.core.membership.Role;
import org.civicops.core.security.*;
import org.springframework.stereotype.Service;

@Service
public class EquipmentAccessService {
  private final CurrentUserProvider current;
  private final OrganizationAccessService access;

  public EquipmentAccessService(CurrentUserProvider c, OrganizationAccessService a) {
    current = c;
    access = a;
  }

  public UUID userId() {
    return current.currentUserId();
  }

  public void requireManageEquipment(UUID org) {
    access.requireAnyRole(userId(), org, Role.EQUIPMENT_MANAGER);
  }

  public void requireReadEquipment(UUID org) {
    access.requireAnyRole(
        userId(),
        org,
        Role.EQUIPMENT_MANAGER,
        Role.PROGRAM_MANAGER,
        Role.EVENT_COORDINATOR,
        Role.VOLUNTEER_COORDINATOR,
        Role.VIEWER);
  }

  public void requireCheckoutManagement(UUID org) {
    requireManageEquipment(org);
  }

  public void requireMaintenanceManagement(UUID org) {
    requireManageEquipment(org);
  }
}
