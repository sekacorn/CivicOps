package org.civicops.facilities.availability.dto;

import java.time.Instant;
import java.util.UUID;

public record AvailabilityResponse(
    UUID spaceId, Instant start, Instant end, boolean available, String reason) {}
