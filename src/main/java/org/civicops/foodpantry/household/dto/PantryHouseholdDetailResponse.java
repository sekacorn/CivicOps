package org.civicops.foodpantry.household.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.foodpantry.household.PantryHousehold;

public record PantryHouseholdDetailResponse(
    UUID id,
    UUID organizationId,
    String externalReferenceNumber,
    String householdName,
    String primaryContactFirstName,
    String primaryContactLastName,
    String email,
    String phone,
    String address,
    int householdSize,
    boolean active,
    String notes,
    Instant createdAt,
    Instant updatedAt,
    long version) {
  public static PantryHouseholdDetailResponse from(PantryHousehold h) {
    return new PantryHouseholdDetailResponse(
        h.getId(),
        h.getOrganization().getId(),
        h.getExternalReferenceNumber(),
        h.getHouseholdName(),
        h.getPrimaryContactFirstName(),
        h.getPrimaryContactLastName(),
        h.getEmail(),
        h.getPhone(),
        h.getAddress(),
        h.getHouseholdSize(),
        h.isActive(),
        h.getNotes(),
        h.getCreatedAt(),
        h.getUpdatedAt(),
        h.getVersion());
  }
}
