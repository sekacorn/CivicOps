package org.civicops.facilities.reporting;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import org.civicops.facilities.reporting.dto.*;
import org.civicops.facilities.security.FacilityAccessService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/facility-reports")
@Tag(name = "Facility Reporting")
public class FacilityReportingController {
  private final FacilityReportingService reports;
  private final FacilityAccessService access;

  public FacilityReportingController(FacilityReportingService r, FacilityAccessService a) {
    reports = r;
    access = a;
  }

  @GetMapping("/summary")
  public FacilityReportSummaryResponse summary(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    access.requireFacilityRead(organizationId);
    return reports.summary(organizationId, from, to);
  }

  @GetMapping("/utilization")
  public FacilityUtilizationResponse utilization(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    access.requireFacilityRead(organizationId);
    return reports.utilization(organizationId, from, to);
  }
}
