package org.civicops.volunteers.volunteer.dto;

import java.time.*;
import java.util.*;
import org.civicops.volunteers.volunteer.*;

public record VolunteerSummaryResponse(
    UUID id,
    String firstName,
    String lastName,
    VolunteerStatus status,
    LocalDate startDate,
    Set<String> skills,
    Instant createdAt) {
  public static VolunteerSummaryResponse from(Volunteer v) {
    return new VolunteerSummaryResponse(
        v.getId(),
        v.getFirstName(),
        v.getLastName(),
        v.getStatus(),
        v.getStartDate(),
        v.getSkills(),
        v.getCreatedAt());
  }
}
