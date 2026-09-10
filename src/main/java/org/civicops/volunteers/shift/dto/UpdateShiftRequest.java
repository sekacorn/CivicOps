package org.civicops.volunteers.shift.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateShiftRequest(
    @Size(min = 1, max = 200) String title,
    Instant startAt,
    Instant endAt,
    @Positive Integer capacity) {}
