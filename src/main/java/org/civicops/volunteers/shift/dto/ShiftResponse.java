package org.civicops.volunteers.shift.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.volunteers.shift.VolunteerShift;

public record ShiftResponse(
    UUID id, UUID opportunityId, String title, Instant startAt, Instant endAt, int capacity) {
  public static ShiftResponse from(VolunteerShift s) {
    return new ShiftResponse(
        s.getId(),
        s.getOpportunity().getId(),
        s.getTitle(),
        s.getStartAt(),
        s.getEndAt(),
        s.getCapacity());
  }
}
