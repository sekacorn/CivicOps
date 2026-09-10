package org.civicops.facilities.space.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.facilities.space.FacilitySpace;

public record FacilitySpaceResponse(
    UUID id,
    UUID facilityId,
    String facilityName,
    String name,
    String description,
    Integer capacity,
    boolean active,
    boolean reservable,
    String locationDetails,
    String accessibilityNotes,
    Instant createdAt,
    Instant updatedAt,
    long version) {
  public static FacilitySpaceResponse from(FacilitySpace s) {
    return new FacilitySpaceResponse(
        s.getId(),
        s.getFacility().getId(),
        s.getFacility().getName(),
        s.getName(),
        s.getDescription(),
        s.getCapacity(),
        s.isActive(),
        s.isReservable(),
        s.getLocationDetails(),
        s.getAccessibilityNotes(),
        s.getCreatedAt(),
        s.getUpdatedAt(),
        s.getVersion());
  }
}
