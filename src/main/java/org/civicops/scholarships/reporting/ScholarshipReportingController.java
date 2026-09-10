package org.civicops.scholarships.reporting;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.civicops.scholarships.reporting.dto.*;
import org.civicops.scholarships.security.ScholarshipAccessService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/scholarship-reports")
@Tag(name = "Scholarship Reporting")
public class ScholarshipReportingController {
  private final ScholarshipReportingService reports;
  private final ScholarshipAccessService access;

  public ScholarshipReportingController(ScholarshipReportingService r, ScholarshipAccessService a) {
    reports = r;
    access = a;
  }

  @GetMapping("/summary")
  public ScholarshipSummaryResponse summary(@PathVariable UUID organizationId) {
    access.requireScholarshipReporting(organizationId);
    return reports.summary(organizationId);
  }

  @GetMapping("/programs/{programId}")
  public ScholarshipProgramReportResponse program(
      @PathVariable UUID organizationId, @PathVariable UUID programId) {
    access.requireScholarshipReporting(organizationId);
    return reports.program(organizationId, programId);
  }

  @GetMapping("/reviews")
  public ScholarshipReviewReportResponse reviews(@PathVariable UUID organizationId) {
    access.requireScholarshipManagement(organizationId);
    return reports.reviews(organizationId);
  }
}
