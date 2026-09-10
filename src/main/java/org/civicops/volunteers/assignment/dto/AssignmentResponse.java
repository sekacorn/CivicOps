package org.civicops.volunteers.assignment.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.volunteers.assignment.*;

public record AssignmentResponse(
    UUID id,
    UUID volunteerId,
    UUID shiftId,
    AssignmentStatus status,
    Instant checkedInAt,
    Instant checkedOutAt) {
  public static AssignmentResponse from(VolunteerAssignment a) {
    return new AssignmentResponse(
        a.getId(),
        a.getVolunteer().getId(),
        a.getShift().getId(),
        a.getStatus(),
        a.getCheckedInAt(),
        a.getCheckedOutAt());
  }
}
