package org.civicops.grants.reporting;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.*;
import org.civicops.grants.reporting.dto.*;
import org.civicops.grants.security.GrantAccessService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
@Tag(name = "Grant Reporting")
public class GrantReportingController {
  private final GrantReportingService service;
  private final GrantAccessService access;

  public GrantReportingController(GrantReportingService service, GrantAccessService access) {
    this.service = service;
    this.access = access;
  }

  @GetMapping("/grant-reports/summary")
  @Operation(
      summary = "Get organization grant portfolio summary",
      description =
          "Reports due soon means a reporting deadline from today through the next 30 days.")
  public OrganizationGrantSummaryResponse summary(@PathVariable UUID organizationId) {
    access.requireRead(organizationId);
    return service.summary(organizationId);
  }

  @GetMapping("/grants/{grantId}/financial-summary")
  @Operation(summary = "Get authoritative grant financial calculations")
  public GrantFinancialSummaryResponse financial(
      @PathVariable UUID organizationId, @PathVariable UUID grantId) {
    access.requireRead(organizationId);
    return service.financial(organizationId, grantId);
  }

  @GetMapping("/grants/{grantId}/expenses/by-category")
  public List<GrantCategoryTotalResponse> byCategory(
      @PathVariable UUID organizationId, @PathVariable UUID grantId) {
    access.requireRead(organizationId);
    return service.byCategory(organizationId, grantId);
  }
}
