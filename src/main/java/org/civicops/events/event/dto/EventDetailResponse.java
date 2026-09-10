package org.civicops.events.event.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.events.event.*;

public record EventDetailResponse(
    UUID id,
    UUID organizationId,
    String name,
    String description,
    EventType eventType,
    String location,
    Instant startDateTime,
    Instant endDateTime,
    Integer capacity,
    boolean registrationRequired,
    Instant registrationDeadline,
    boolean waitlistEnabled,
    EventStatus status,
    UUID linkedGrantId,
    String linkedGrantName,
    UUID linkedDonationCampaignId,
    String linkedDonationCampaignName,
    Instant createdAt,
    Instant updatedAt,
    long version) {
  public static EventDetailResponse from(EventRecord e) {
    return new EventDetailResponse(
        e.getId(),
        e.getOrganization().getId(),
        e.getName(),
        e.getDescription(),
        e.getEventType(),
        e.getLocation(),
        e.getStartDateTime(),
        e.getEndDateTime(),
        e.getCapacity(),
        e.isRegistrationRequired(),
        e.getRegistrationDeadline(),
        e.isWaitlistEnabled(),
        e.getStatus(),
        e.getLinkedGrant() == null ? null : e.getLinkedGrant().getId(),
        e.getLinkedGrant() == null ? null : e.getLinkedGrant().getGrantName(),
        e.getLinkedDonationCampaign() == null ? null : e.getLinkedDonationCampaign().getId(),
        e.getLinkedDonationCampaign() == null ? null : e.getLinkedDonationCampaign().getName(),
        e.getCreatedAt(),
        e.getUpdatedAt(),
        e.getVersion());
  }
}
