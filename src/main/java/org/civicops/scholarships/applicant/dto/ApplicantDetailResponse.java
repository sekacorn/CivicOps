package org.civicops.scholarships.applicant.dto;

import java.time.*;
import java.util.UUID;
import org.civicops.scholarships.applicant.ScholarshipApplicant;

public record ApplicantDetailResponse(
    UUID id,
    UUID organizationId,
    UUID userId,
    String firstName,
    String lastName,
    String preferredName,
    String email,
    String phone,
    LocalDate dateOfBirth,
    String address,
    String schoolName,
    Integer graduationYear,
    String studentId,
    String notes,
    Instant createdAt,
    Instant updatedAt,
    long version) {
  public static ApplicantDetailResponse from(ScholarshipApplicant a) {
    return new ApplicantDetailResponse(
        a.getId(),
        a.getOrganization().getId(),
        a.getUser() == null ? null : a.getUser().getId(),
        a.getFirstName(),
        a.getLastName(),
        a.getPreferredName(),
        a.getEmail(),
        a.getPhone(),
        a.getDateOfBirth(),
        a.getAddress(),
        a.getSchoolName(),
        a.getGraduationYear(),
        a.getStudentId(),
        a.getNotes(),
        a.getCreatedAt(),
        a.getUpdatedAt(),
        a.getVersion());
  }
}
