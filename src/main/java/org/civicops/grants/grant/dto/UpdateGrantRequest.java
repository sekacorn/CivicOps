package org.civicops.grants.grant.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateGrantRequest(
    @Size(min = 1, max = 200) String grantName,
    @Size(min = 1, max = 200) String grantorName,
    @Size(max = 100) String grantNumber,
    String description,
    @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal awardAmount,
    LocalDate applicationDeadline,
    LocalDate startDate,
    LocalDate endDate,
    LocalDate reportingDeadline,
    Boolean restricted,
    String restrictionDescription,
    @Size(max = 200) String primaryContactName,
    @Email @Size(max = 320) String primaryContactEmail,
    String notes) {}
