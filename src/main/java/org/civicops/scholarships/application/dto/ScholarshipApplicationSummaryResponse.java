package org.civicops.scholarships.application.dto;

import java.time.*;
import java.util.UUID;
import org.civicops.scholarships.application.*;

public record ScholarshipApplicationSummaryResponse(
    UUID id,
    UUID programId,
    String programName,
    UUID applicantId,
    String applicantDisplayName,
    ScholarshipApplicationStatus status,
    boolean eligibilityConfirmed,
    Instant submittedAt,
    Instant createdAt) {
  public static ScholarshipApplicationSummaryResponse from(ScholarshipApplication a) {
    return new ScholarshipApplicationSummaryResponse(
        a.getId(),
        a.getProgram().getId(),
        a.getProgram().getName(),
        a.getApplicant().getId(),
        a.getApplicant().displayName(),
        a.getStatus(),
        a.isEligibilityConfirmed(),
        a.getSubmittedAt(),
        a.getCreatedAt());
  }
}
