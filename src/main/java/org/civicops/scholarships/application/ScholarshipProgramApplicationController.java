package org.civicops.scholarships.application;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.civicops.scholarships.application.dto.*;
import org.civicops.scholarships.security.ScholarshipAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
    "/api/v1/organizations/{organizationId}/scholarship-programs/{programId}/applications")
@Tag(name = "Scholarship Applications")
public class ScholarshipProgramApplicationController {
  private final ScholarshipApplicationService applications;
  private final ScholarshipAccessService access;

  public ScholarshipProgramApplicationController(
      ScholarshipApplicationService a, ScholarshipAccessService x) {
    applications = a;
    access = x;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ScholarshipApplicationDetailResponse create(
      @PathVariable UUID organizationId,
      @PathVariable UUID programId,
      @Valid @RequestBody CreateScholarshipApplicationRequest r) {
    access.requireScholarshipManagement(organizationId);
    return applications.create(organizationId, programId, r);
  }

  @GetMapping
  public Page<ScholarshipApplicationSummaryResponse> list(
      @PathVariable UUID organizationId,
      @PathVariable UUID programId,
      @RequestParam(required = false) ScholarshipApplicationStatus status,
      @RequestParam(required = false) UUID applicantId,
      @RequestParam(required = false) java.time.Instant submittedFrom,
      @RequestParam(required = false) java.time.Instant submittedTo,
      @RequestParam(required = false) Boolean eligibilityConfirmed,
      @PageableDefault(size = 20, sort = "createdAt") Pageable p) {
    access.requireScholarshipManagement(organizationId);
    return applications.list(
        organizationId,
        programId,
        status,
        applicantId,
        submittedFrom,
        submittedTo,
        eligibilityConfirmed,
        SafePageables.allow(p, Set.of("submittedAt", "status", "createdAt")));
  }
}
