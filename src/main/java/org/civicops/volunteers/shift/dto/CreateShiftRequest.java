package org.civicops.volunteers.shift.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;

public record CreateShiftRequest(
    @NotBlank @Size(max = 200) String title,
    @NotNull Instant startAt,
    @NotNull Instant endAt,
    @Positive int capacity) {}
