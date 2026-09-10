package org.civicops.volunteers.opportunity.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.volunteers.opportunity.*;

public record OpportunityResponse(
    UUID id,
    UUID organizationId,
    String title,
    String description,
    String location,
    Instant startAt,
    Instant endAt,
    Integer minimumVolunteers,
    Integer maximumVolunteers,
    OpportunityStatus status,
    UUID eventId) {
  public static OpportunityResponse from(VolunteerOpportunity o) {
    return new OpportunityResponse(
        o.getId(),
        o.getOrganization().getId(),
        o.getTitle(),
        o.getDescription(),
        o.getLocation(),
        o.getStartAt(),
        o.getEndAt(),
        o.getMinimumVolunteers(),
        o.getMaximumVolunteers(),
        o.getStatus(),
        o.getEvent() == null ? null : o.getEvent().getId());
  }
}
