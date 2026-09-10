package org.civicops.volunteers.hours.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateHourEntryRequest(
    @NotNull UUID volunteerId,
    UUID assignmentId,
    @NotNull @PastOrPresent LocalDate serviceDate,
    @NotNull @DecimalMin("0.01") @DecimalMax("24.00") @Digits(integer = 2, fraction = 2)
        BigDecimal hours,
    @Size(max = 500) String description) {}
