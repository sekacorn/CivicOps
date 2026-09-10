package org.civicops.scholarships.award.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.civicops.scholarships.award.*;

public record ScholarshipAwardResponse(
    UUID id,
    UUID programId,
    String programName,
    UUID applicationId,
    UUID applicantId,
    String applicantDisplayName,
    BigDecimal amount,
    LocalDate awardDate,
    ScholarshipAwardStatus status,
    String notes,
    Instant createdAt,
    Instant updatedAt,
    long version) {
  public static ScholarshipAwardResponse from(ScholarshipAward a) {
    return new ScholarshipAwardResponse(
        a.getId(),
        a.getProgram().getId(),
        a.getProgram().getName(),
        a.getApplication().getId(),
        a.getApplicant().getId(),
        a.getApplicant().displayName(),
        a.getAmount(),
        a.getAwardDate(),
        a.getStatus(),
        a.getNotes(),
        a.getCreatedAt(),
        a.getUpdatedAt(),
        a.getVersion());
  }
}
