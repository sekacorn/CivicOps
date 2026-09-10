package org.civicops.foodpantry.pantry.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.foodpantry.pantry.FoodPantryLocation;

public record PantryLocationResponse(
    UUID id,
    UUID organizationId,
    String name,
    String description,
    String addressLine1,
    String addressLine2,
    String city,
    String state,
    String postalCode,
    String country,
    boolean active,
    String timezone,
    String notes,
    Instant createdAt,
    Instant updatedAt,
    long version) {
  public static PantryLocationResponse from(FoodPantryLocation p) {
    return new PantryLocationResponse(
        p.getId(),
        p.getOrganization().getId(),
        p.getName(),
        p.getDescription(),
        p.getAddressLine1(),
        p.getAddressLine2(),
        p.getCity(),
        p.getState(),
        p.getPostalCode(),
        p.getCountry(),
        p.isActive(),
        p.getTimezone(),
        p.getNotes(),
        p.getCreatedAt(),
        p.getUpdatedAt(),
        p.getVersion());
  }
}
