package org.civicops.equipment.maintenance;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.*;
import org.civicops.equipment.maintenance.dto.*;
import org.civicops.equipment.security.EquipmentAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/equipment-maintenance")
@Tag(name = "Equipment Maintenance")
public class EquipmentMaintenanceController {
  private final EquipmentMaintenanceService maintenance;
  private final EquipmentAccessService access;

  public EquipmentMaintenanceController(EquipmentMaintenanceService m, EquipmentAccessService a) {
    maintenance = m;
    access = a;
  }

  @GetMapping
  public Page<EquipmentMaintenanceResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) MaintenanceStatus status,
      @RequestParam(required = false) MaintenanceType type,
      @RequestParam(required = false) UUID assetId,
      @RequestParam(required = false) Instant startedFrom,
      @RequestParam(required = false) Instant startedTo,
      @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) Pageable p) {
    access.requireMaintenanceManagement(organizationId);
    return maintenance.list(
        organizationId,
        assetId,
        status,
        type,
        startedFrom,
        startedTo,
        SafePageables.allow(p, Set.of("startedAt", "completedAt", "createdAt")));
  }

  @GetMapping("/{maintenanceId}")
  public EquipmentMaintenanceResponse detail(
      @PathVariable UUID organizationId, @PathVariable UUID maintenanceId) {
    access.requireMaintenanceManagement(organizationId);
    return maintenance.detail(organizationId, maintenanceId);
  }

  @PostMapping("/{maintenanceId}/start")
  public EquipmentMaintenanceResponse start(
      @PathVariable UUID organizationId, @PathVariable UUID maintenanceId) {
    access.requireMaintenanceManagement(organizationId);
    return maintenance.start(organizationId, maintenanceId);
  }

  @PostMapping("/{maintenanceId}/complete")
  public EquipmentMaintenanceResponse complete(
      @PathVariable UUID organizationId,
      @PathVariable UUID maintenanceId,
      @Valid @RequestBody CompleteMaintenanceRequest r) {
    access.requireMaintenanceManagement(organizationId);
    return maintenance.complete(organizationId, maintenanceId, access.userId(), r);
  }

  @PostMapping("/{maintenanceId}/cancel")
  public EquipmentMaintenanceResponse cancel(
      @PathVariable UUID organizationId, @PathVariable UUID maintenanceId) {
    access.requireMaintenanceManagement(organizationId);
    return maintenance.cancel(organizationId, maintenanceId);
  }
}
