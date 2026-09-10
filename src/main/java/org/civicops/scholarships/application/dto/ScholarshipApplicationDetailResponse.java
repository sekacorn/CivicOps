package org.civicops.scholarships.application.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.civicops.scholarships.applicant.dto.ApplicantDetailResponse;
import org.civicops.scholarships.application.*;

public record ScholarshipApplicationDetailResponse(
    UUID id,
    UUID organizationId,
    UUID programId,
    String programName,
    ApplicantDetailResponse applicant,
    Instant submittedAt,
    ScholarshipApplicationStatus status,
    boolean eligibilityConfirmed,
    String eligibilityNotes,
    String personalStatement,
    String financialNeedStatement,
    BigDecimal gpa,
    BigDecimal householdIncome,
    BigDecimal requestedAmount,
    Instant createdAt,
    Instant updatedAt,
    long version) {
  public static ScholarshipApplicationDetailResponse from(ScholarshipApplication a) {
    return new ScholarshipApplicationDetailResponse(
        a.getId(),
        a.getOrganization().getId(),
        a.getProgram().getId(),
        a.getProgram().getName(),
        ApplicantDetailResponse.from(a.getApplicant()),
        a.getSubmittedAt(),
        a.getStatus(),
        a.isEligibilityConfirmed(),
        a.getEligibilityNotes(),
        a.getPersonalStatement(),
        a.getFinancialNeedStatement(),
        a.getGpa(),
        a.getHouseholdIncome(),
        a.getRequestedAmount(),
        a.getCreatedAt(),
        a.getUpdatedAt(),
        a.getVersion());
  }
}
