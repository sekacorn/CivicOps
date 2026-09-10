package org.civicops.events.event.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;
import org.civicops.events.event.EventType;

public record UpdateEventRequest(
    @Size(max = 200) String name,
    @Size(max = 5000) String description,
    EventType eventType,
    @Size(max = 300) String location,
    Instant startDateTime,
    Instant endDateTime,
    @Positive Integer capacity,
    Boolean registrationRequired,
    Instant registrationDeadline,
    Boolean waitlistEnabled,
    UUID linkedGrantId,
    UUID linkedDonationCampaignId) {}
