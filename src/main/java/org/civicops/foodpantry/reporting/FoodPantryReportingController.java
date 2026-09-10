package org.civicops.foodpantry.reporting;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import org.civicops.foodpantry.reporting.dto.*;
import org.civicops.foodpantry.security.FoodPantryAccessService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/food-pantry-reports")
@Tag(name = "Food Pantry Reporting")
public class FoodPantryReportingController {
  private final FoodPantryReportingService reports;
  private final FoodPantryAccessService access;

  public FoodPantryReportingController(FoodPantryReportingService r, FoodPantryAccessService a) {
    reports = r;
    access = a;
  }

  @GetMapping("/inventory-summary")
  public PantryInventorySummaryResponse inventory(
      @PathVariable UUID organizationId, @RequestParam(required = false) UUID pantryId) {
    access.requirePantryReporting(organizationId);
    return reports.inventory(organizationId, pantryId);
  }

  @GetMapping("/distributions")
  public PantryDistributionReportResponse distributions(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) UUID pantryId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    access.requirePantryReporting(organizationId);
    return reports.distributions(organizationId, pantryId, from, to);
  }

  @GetMapping("/households")
  public PantryHouseholdReportResponse households(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    access.requirePantryReporting(organizationId);
    return reports.households(organizationId, from, to);
  }

  @GetMapping("/waste")
  public PantryWasteReportResponse waste(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) UUID pantryId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    access.requirePantryReporting(organizationId);
    return reports.waste(organizationId, pantryId, from, to);
  }
}
