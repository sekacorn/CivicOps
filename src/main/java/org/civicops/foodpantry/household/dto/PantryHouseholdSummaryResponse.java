package org.civicops.foodpantry.household.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.foodpantry.household.PantryHousehold;

public record PantryHouseholdSummaryResponse(
    UUID id,
    String externalReferenceNumber,
    String displayName,
    int householdSize,
    boolean active,
    Instant createdAt) {
  public static PantryHouseholdSummaryResponse from(PantryHousehold h) {
    return new PantryHouseholdSummaryResponse(
        h.getId(),
        h.getExternalReferenceNumber(),
        h.displayName(),
        h.getHouseholdSize(),
        h.isActive(),
        h.getCreatedAt());
  }
}
