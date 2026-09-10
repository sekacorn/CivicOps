package org.civicops.facilities.facility.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.facilities.facility.*;

public record FacilityResponse(
    UUID id,
    UUID organizationId,
    String name,
    String description,
    FacilityType facilityType,
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
  public static FacilityResponse from(Facility f) {
    return new FacilityResponse(
        f.getId(),
        f.getOrganization().getId(),
        f.getName(),
        f.getDescription(),
        f.getFacilityType(),
        f.getAddressLine1(),
        f.getAddressLine2(),
        f.getCity(),
        f.getState(),
        f.getPostalCode(),
        f.getCountry(),
        f.isActive(),
        f.getTimezone(),
        f.getNotes(),
        f.getCreatedAt(),
        f.getUpdatedAt(),
        f.getVersion());
  }
}
