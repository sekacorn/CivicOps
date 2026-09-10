package org.civicops.cases.service.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.civicops.cases.service.CaseServiceType;

public record CreateCaseServiceRequest(
    @NotNull CaseServiceType serviceType,
    @Size(max = 10000) String description,
    @NotNull LocalDate serviceDate,
    @PositiveOrZero BigDecimal quantity,
    @Size(max = 50) String unit,
    @PositiveOrZero BigDecimal valueAmount,
    UUID providedByUserId,
    @Size(max = 10000) String notes) {}
