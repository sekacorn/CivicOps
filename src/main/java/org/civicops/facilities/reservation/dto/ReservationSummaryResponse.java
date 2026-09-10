package org.civicops.facilities.reservation.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.facilities.reservation.*;

public record ReservationSummaryResponse(
    UUID id,
    UUID facilityId,
    String facilityName,
    UUID spaceId,
    String spaceName,
    String title,
    Instant startDateTime,
    Instant endDateTime,
    Integer expectedAttendance,
    ReservationStatus status,
    Instant requestedAt,
    UUID eventId) {
  public static ReservationSummaryResponse from(FacilityReservation r) {
    var s = r.getFacilitySpace();
    return new ReservationSummaryResponse(
        r.getId(),
        s.getFacility().getId(),
        s.getFacility().getName(),
        s.getId(),
        s.getName(),
        r.getTitle(),
        r.getStartDateTime(),
        r.getEndDateTime(),
        r.getExpectedAttendance(),
        r.getStatus(),
        r.getRequestedAt(),
        r.getEvent() == null ? null : r.getEvent().getId());
  }
}
