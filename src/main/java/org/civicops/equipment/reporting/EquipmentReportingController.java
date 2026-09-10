package org.civicops.equipment.reporting;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import org.civicops.equipment.reporting.dto.*;
import org.civicops.equipment.security.EquipmentAccessService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/equipment-reports")
@Tag(name = "Equipment Reporting")
public class EquipmentReportingController {
  private final EquipmentReportingService reports;
  private final EquipmentAccessService access;

  public EquipmentReportingController(EquipmentReportingService r, EquipmentAccessService a) {
    reports = r;
    access = a;
  }

  @GetMapping("/summary")
  public EquipmentInventorySummaryResponse summary(@PathVariable UUID organizationId) {
    access.requireReadEquipment(organizationId);
    return reports.summary(organizationId);
  }

  @GetMapping("/utilization")
  public EquipmentUtilizationResponse utilization(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    access.requireReadEquipment(organizationId);
    return reports.utilization(organizationId, from, to);
  }

  @GetMapping("/maintenance")
  public EquipmentMaintenanceReportResponse maintenance(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    access.requireReadEquipment(organizationId);
    return reports.maintenance(organizationId, from, to);
  }
}
