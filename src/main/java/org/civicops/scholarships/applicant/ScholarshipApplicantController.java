package org.civicops.scholarships.applicant;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.civicops.scholarships.applicant.dto.*;
import org.civicops.scholarships.security.ScholarshipAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/scholarship-applicants")
@Tag(name = "Scholarship Applicants")
public class ScholarshipApplicantController {
  private final ScholarshipApplicantService applicants;
  private final ScholarshipAccessService access;

  public ScholarshipApplicantController(ScholarshipApplicantService a, ScholarshipAccessService x) {
    applicants = a;
    access = x;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApplicantDetailResponse create(
      @PathVariable UUID organizationId, @Valid @RequestBody CreateScholarshipApplicantRequest r) {
    access.requireScholarshipManagement(organizationId);
    return applicants.create(organizationId, r);
  }

  @GetMapping
  public Page<ApplicantSummaryResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) String schoolName,
      @RequestParam(required = false) Integer graduationYear,
      @RequestParam(required = false) String email,
      @PageableDefault(size = 20, sort = "lastName") Pageable p) {
    access.requireProgramRead(organizationId);
    if (email != null) access.requireScholarshipManagement(organizationId);
    return applicants.list(
        organizationId,
        schoolName,
        graduationYear,
        email,
        SafePageables.allow(p, Set.of("lastName", "graduationYear", "createdAt")));
  }

  @GetMapping("/{applicantId}")
  public ApplicantDetailResponse detail(
      @PathVariable UUID organizationId, @PathVariable UUID applicantId) {
    access.requireApplicantDetailAccess(organizationId);
    return applicants.detail(organizationId, applicantId);
  }

  @PatchMapping("/{applicantId}")
  public ApplicantDetailResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID applicantId,
      @Valid @RequestBody UpdateScholarshipApplicantRequest r) {
    access.requireScholarshipManagement(organizationId);
    return applicants.update(organizationId, applicantId, r);
  }
}
