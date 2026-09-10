package org.civicops.volunteers.opportunity.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public record CreateOpportunityRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 5000) String description,
    @Size(max = 300) String location,
    @NotNull @Future Instant startAt,
    @NotNull @Future Instant endAt,
    @PositiveOrZero Integer minimumVolunteers,
    @Positive Integer maximumVolunteers,
    UUID eventId) {
  public CreateOpportunityRequest(
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
