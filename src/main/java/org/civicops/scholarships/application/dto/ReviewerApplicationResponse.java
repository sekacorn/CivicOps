package org.civicops.scholarships.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.civicops.scholarships.application.*;

public record ReviewerApplicationResponse(
    UUID id,
    UUID programId,
    String programName,
    UUID applicantId,
    String applicantDisplayName,
    String schoolName,
    Integer graduationYear,
    ScholarshipApplicationStatus status,
    boolean eligibilityConfirmed,
    String personalStatement,
    String financialNeedStatement,
    BigDecimal gpa,
    BigDecimal requestedAmount,
    Instant submittedAt) {
  public static ReviewerApplicationResponse from(ScholarshipApplication a) {
    return new ReviewerApplicationResponse(
        a.getId(),
        a.getProgram().getId(),
        a.getProgram().getName(),
        a.getApplicant().getId(),
        a.getApplicant().displayName(),
        a.getApplicant().getSchoolName(),
        a.getApplicant().getGraduationYear(),
        a.getStatus(),
        a.isEligibilityConfirmed(),
        a.getPersonalStatement(),
        a.getFinancialNeedStatement(),
        a.getGpa(),
        a.getRequestedAmount(),
        a.getSubmittedAt());
  }
}
