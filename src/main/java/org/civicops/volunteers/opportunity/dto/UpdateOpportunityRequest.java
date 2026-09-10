package org.civicops.volunteers.opportunity.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record UpdateOpportunityRequest(
    @Size(min = 1, max = 200) String title,
    @Size(max = 5000) String description,
    @Size(max = 300) String location,
    Instant startAt,
    Instant endAt,
    @PositiveOrZero Integer minimumVolunteers,
    @Positive Integer maximumVolunteers,
    UUID eventId) {
  public UpdateOpportunityRequest(
      String title,
      String description,
      String location,
      Instant startAt,
      Instant endAt,
      Integer minimumVolunteers,
      Integer maximumVolunteers) {
    this(title, description, location, startAt, endAt, minimumVolunteers, maximumVolunteers, null);
  }
}
