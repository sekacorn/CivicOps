package org.civicops.scholarships.application.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateScholarshipApplicationRequest(
    @NotNull UUID applicantId,
    Boolean eligibilityConfirmed,
    @Size(max = 10000) String eligibilityNotes,
    @Size(max = 50000) String personalStatement,
    @Size(max = 50000) String financialNeedStatement,
    @DecimalMin("0.00") @DecimalMax("4.00") BigDecimal gpa,
    @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal householdIncome,
    @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 17, fraction = 2)
        BigDecimal requestedAmount) {}
