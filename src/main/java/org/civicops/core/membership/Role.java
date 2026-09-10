package org.civicops.core.membership;

import java.util.Set;

public enum Role {
  SYSTEM_ADMIN(false),
  ORG_ADMIN(true),
  PROGRAM_MANAGER(true),
  GRANT_MANAGER(true),
  DONATION_MANAGER(true),
  VOLUNTEER_COORDINATOR(true),
  EVENT_COORDINATOR(true),
  CASE_MANAGER(true),
  CASE_WORKER(true),
  EQUIPMENT_MANAGER(true),
  FACILITY_MANAGER(true),
  SCHOLARSHIP_MANAGER(true),
  SCHOLARSHIP_REVIEWER(true),
  FOOD_PANTRY_MANAGER(true),
  BOARD_MANAGER(true),
  BOARD_MEMBER(true),
  VOLUNTEER(true),
  VIEWER(true);

  private final boolean organizationAssignable;

  Role(boolean organizationAssignable) {
    this.organizationAssignable = organizationAssignable;
  }

  public boolean isOrganizationAssignable() {
    return organizationAssignable;
  }

  public boolean grantsAny(Set<Role> allowedRoles) {
    return this == ORG_ADMIN || allowedRoles.contains(this);
  }
}
