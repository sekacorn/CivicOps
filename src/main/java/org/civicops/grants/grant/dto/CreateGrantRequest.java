package org.civicops.grants.grant.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateGrantRequest(
    @NotBlank @Size(max = 200) String grantName,
    @NotBlank @Size(max = 200) String grantorName,
    @Size(max = 100) String grantNumber,
    String description,
    @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal awardAmount,
    LocalDate applicationDeadline,
    LocalDate submittedDate,
    LocalDate awardDate,
    LocalDate startDate,
    LocalDate endDate,
    LocalDate reportingDeadline,
    boolean restricted,
    String restrictionDescription,
    @Size(max = 200) String primaryContactName,
    @Email @Size(max = 320) String primaryContactEmail,
    String notes) {}
