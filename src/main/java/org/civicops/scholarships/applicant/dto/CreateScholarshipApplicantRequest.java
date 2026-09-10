package org.civicops.scholarships.applicant.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

public record CreateScholarshipApplicantRequest(
    UUID userId,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @Size(max = 100) String preferredName,
    @NotBlank @Email @Size(max = 320) String email,
    @Size(max = 50) String phone,
    @Past LocalDate dateOfBirth,
    @Size(max = 5000) String address,
    @Size(max = 200) String schoolName,
    @Min(1900) @Max(2200) Integer graduationYear,
    @Size(max = 100) String studentId,
    @Size(max = 10000) String notes) {}
