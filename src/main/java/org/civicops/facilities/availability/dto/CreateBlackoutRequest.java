package org.civicops.facilities.availability.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public record CreateBlackoutRequest(
    @NotNull UUID facilityId,
    UUID facilitySpaceId,
    @NotNull Instant startDateTime,
    @NotNull Instant endDateTime,
    @NotBlank @Size(max = 1000) String reason) {}
