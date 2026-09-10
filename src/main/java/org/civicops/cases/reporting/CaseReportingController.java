package org.civicops.cases.reporting;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.*;
import org.civicops.cases.reporting.dto.*;
import org.civicops.cases.security.CaseAccessService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/case-reports")
@Tag(name = "Case Management Reporting")
public class CaseReportingController {
  private final CaseReportingService reports;
  private final CaseAccessService access;

  public CaseReportingController(CaseReportingService r, CaseAccessService a) {
    reports = r;
    access = a;
  }

  @GetMapping("/summary")
  public CaseReportSummaryResponse summary(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    access.requireReporting(organizationId);
    return reports.summary(organizationId, from, to);
  }

  @GetMapping("/workload")
  public List<CaseWorkloadResponse> workload(@PathVariable UUID organizationId) {
    access.requireWorkloadReporting(organizationId);
    return reports.workload(organizationId);
  }

  @GetMapping("/services")
  public List<CaseServiceTypeReportResponse> services(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    access.requireReporting(organizationId);
    return reports.services(organizationId, from, to);
  }
}
