package org.civicops.scholarships.program.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.civicops.scholarships.program.*;

public record ScholarshipProgramResponse(
    UUID id,
    UUID organizationId,
    String name,
    String description,
    String academicYear,
    LocalDate applicationOpenDate,
    LocalDate applicationDeadline,
    BigDecimal awardAmount,
    Integer numberOfAwards,
    String eligibilityDescription,
    ScholarshipProgramStatus status,
    Instant createdAt,
    Instant updatedAt,
    long version) {
  public static ScholarshipProgramResponse from(ScholarshipProgram p) {
    return new ScholarshipProgramResponse(
        p.getId(),
        p.getOrganization().getId(),
        p.getName(),
        p.getDescription(),
        p.getAcademicYear(),
        p.getApplicationOpenDate(),
        p.getApplicationDeadline(),
        p.getAwardAmount(),
        p.getNumberOfAwards(),
        p.getEligibilityDescription(),
        p.getStatus(),
        p.getCreatedAt(),
        p.getUpdatedAt(),
        p.getVersion());
  }
}
