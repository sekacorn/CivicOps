package org.civicops.volunteers.hours.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.civicops.volunteers.hours.*;

public record HourEntryResponse(
    UUID id,
    UUID volunteerId,
    UUID assignmentId,
    LocalDate serviceDate,
    BigDecimal hours,
    String description,
    HourEntryStatus status,
    UUID reviewedByUserId,
    Instant reviewedAt,
    String rejectionReason) {
  public static HourEntryResponse from(VolunteerHourEntry h) {
    return new HourEntryResponse(
        h.getId(),
        h.getVolunteer().getId(),
        h.getAssignment() == null ? null : h.getAssignment().getId(),
        h.getServiceDate(),
        h.getHours(),
        h.getDescription(),
        h.getStatus(),
        h.getReviewedByUser() == null ? null : h.getReviewedByUser().getId(),
        h.getReviewedAt(),
        h.getRejectionReason());
  }
}
