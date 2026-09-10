package org.civicops.foodpantry.distribution.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public record CreateDistributionVisitRequest(
    UUID householdId,
    @Size(max = 200) String recipientName,
    @NotNull Instant visitDateTime,
    @Min(1) Integer householdSizeAtVisit,
    @Size(max = 5000) String notes) {}
