package org.civicops.scholarships.program.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateScholarshipProgramRequest(
    @Size(max = 200) String name,
    @Size(max = 10000) String description,
    @Size(max = 30) String academicYear,
    LocalDate applicationOpenDate,
    LocalDate applicationDeadline,
    @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal awardAmount,
    @Positive Integer numberOfAwards,
    @Size(max = 10000) String eligibilityDescription) {}
