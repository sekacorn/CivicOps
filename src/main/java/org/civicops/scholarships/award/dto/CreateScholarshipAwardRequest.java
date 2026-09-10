package org.civicops.scholarships.award.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateScholarshipAwardRequest(
    @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 17, fraction = 2)
        BigDecimal amount,
    @NotNull LocalDate awardDate,
    @Size(max = 10000) String notes) {}
