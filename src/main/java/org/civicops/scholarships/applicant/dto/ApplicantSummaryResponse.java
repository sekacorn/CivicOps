package org.civicops.scholarships.applicant.dto;

import java.util.UUID;
import org.civicops.scholarships.applicant.ScholarshipApplicant;

public record ApplicantSummaryResponse(
    UUID id, String displayName, String schoolName, Integer graduationYear) {
  public static ApplicantSummaryResponse from(ScholarshipApplicant a) {
    return new ApplicantSummaryResponse(
        a.getId(), a.displayName(), a.getSchoolName(), a.getGraduationYear());
  }
}
