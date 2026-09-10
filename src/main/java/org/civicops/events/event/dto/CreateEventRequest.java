package org.civicops.events.event.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;
import org.civicops.events.event.EventType;

public record CreateEventRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 5000) String description,
    @NotNull EventType eventType,
    @Size(max = 300) String location,
    @NotNull Instant startDateTime,
    @NotNull Instant endDateTime,
    @Positive Integer capacity,
    boolean registrationRequired,
    Instant registrationDeadline,
    boolean waitlistEnabled,
    UUID linkedGrantId,
    UUID linkedDonationCampaignId) {}
