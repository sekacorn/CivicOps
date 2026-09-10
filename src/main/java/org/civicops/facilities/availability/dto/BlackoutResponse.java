package org.civicops.facilities.availability.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.facilities.availability.FacilityBlackout;

public record BlackoutResponse(
    UUID id,
    UUID facilityId,
    UUID facilitySpaceId,
    Instant startDateTime,
    Instant endDateTime,
    String reason,
    boolean active,
    Instant cancelledAt,
    Instant createdAt) {
  public static BlackoutResponse from(FacilityBlackout b) {
    return new BlackoutResponse(
        b.getId(),
        b.getFacility().getId(),
        b.getFacilitySpace() == null ? null : b.getFacilitySpace().getId(),
        b.getStartDateTime(),
        b.getEndDateTime(),
        b.getReason(),
        b.isActive(),
        b.getCancelledAt(),
        b.getCreatedAt());
  }
}
