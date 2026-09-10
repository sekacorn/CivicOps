package org.civicops.facilities.reservation.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.facilities.reservation.*;

public record ReservationDetailResponse(
    UUID id,
    UUID organizationId,
    UUID facilityId,
    String facilityName,
    UUID spaceId,
    String spaceName,
    UUID requestedByUserId,
    String requesterName,
    String requesterEmail,
    UUID eventId,
    String title,
    String purpose,
    Instant startDateTime,
    Instant endDateTime,
    Integer expectedAttendance,
    ReservationStatus status,
    Instant requestedAt,
    UUID approvedByUserId,
    Instant approvedAt,
    UUID rejectedByUserId,
    Instant rejectedAt,
    String rejectionReason,
    Instant cancelledAt,
    String cancellationReason,
    String notes,
    Instant createdAt,
    Instant updatedAt,
    long version) {
  public static ReservationDetailResponse from(FacilityReservation r) {
    var s = r.getFacilitySpace();
    return new ReservationDetailResponse(
        r.getId(),
        r.getOrganization().getId(),
        s.getFacility().getId(),
        s.getFacility().getName(),
        s.getId(),
        s.getName(),
        r.getRequestedByUser() == null ? null : r.getRequestedByUser().getId(),
        r.getRequesterName(),
        r.getRequesterEmail(),
        r.getEvent() == null ? null : r.getEvent().getId(),
        r.getTitle(),
        r.getPurpose(),
        r.getStartDateTime(),
        r.getEndDateTime(),
        r.getExpectedAttendance(),
        r.getStatus(),
        r.getRequestedAt(),
        r.getApprovedBy() == null ? null : r.getApprovedBy().getId(),
        r.getApprovedAt(),
        r.getRejectedBy() == null ? null : r.getRejectedBy().getId(),
        r.getRejectedAt(),
        r.getRejectionReason(),
        r.getCancelledAt(),
        r.getCancellationReason(),
        r.getNotes(),
        r.getCreatedAt(),
        r.getUpdatedAt(),
        r.getVersion());
  }
}
