package org.civicops.equipment.asset;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.*;
import java.util.*;
import org.civicops.equipment.asset.dto.*;
import org.civicops.equipment.checkout.*;
import org.civicops.equipment.checkout.dto.*;
import org.civicops.equipment.maintenance.*;
import org.civicops.equipment.maintenance.dto.*;
import org.civicops.equipment.security.EquipmentAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/equipment")
@Tag(name = "Equipment Inventory")
public class EquipmentAssetController {
  private final EquipmentAssetService assets;
  private final EquipmentCheckoutService checkouts;
  private final EquipmentMaintenanceService maintenance;
  private final EquipmentAccessService access;

  public EquipmentAssetController(
      EquipmentAssetService a,
      EquipmentCheckoutService c,
      EquipmentMaintenanceService m,
      EquipmentAccessService x) {
    assets = a;
    checkouts = c;
    maintenance = m;
    access = x;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EquipmentAssetDetailResponse create(
      @PathVariable UUID organizationId, @Valid @RequestBody CreateEquipmentAssetRequest r) {
    access.requireManageEquipment(organizationId);
    return assets.create(organizationId, r);
  }

  @GetMapping
  public Page<EquipmentAssetSummaryResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) AssetStatus status,
      @RequestParam(required = false) EquipmentCondition condition,
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) String assetTag,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Boolean available,
      @PageableDefault(size = 20, sort = "assetTag") Pageable p) {
    access.requireReadEquipment(organizationId);
    return assets.list(
        organizationId,
        status,
        condition,
        categoryId,
        assetTag,
        name,
        available,
        SafePageables.allow(p, Set.of("assetTag", "name", "status", "condition", "createdAt")));
  }

  @GetMapping("/{assetId}")
  public EquipmentAssetDetailResponse detail(
      @PathVariable UUID organizationId, @PathVariable UUID assetId) {
    access.requireReadEquipment(organizationId);
    return assets.detail(organizationId, assetId);
  }

  @PatchMapping("/{assetId}")
  public EquipmentAssetDetailResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID assetId,
      @Valid @RequestBody UpdateEquipmentAssetRequest r) {
    access.requireManageEquipment(organizationId);
    return assets.update(organizationId, assetId, r);
  }

  @PostMapping("/{assetId}/retire")
  public EquipmentAssetDetailResponse retire(
      @PathVariable UUID organizationId, @PathVariable UUID assetId) {
    access.requireManageEquipment(organizationId);
    return assets.retire(organizationId, assetId);
  }

  @PostMapping("/{assetId}/checkouts")
  @ResponseStatus(HttpStatus.CREATED)
  public EquipmentCheckoutDetailResponse checkout(
      @PathVariable UUID organizationId,
      @PathVariable UUID assetId,
      @Valid @RequestBody CreateEquipmentCheckoutRequest r) {
    access.requireCheckoutManagement(organizationId);
    return checkouts.checkout(organizationId, assetId, access.userId(), r);
  }

  @GetMapping("/{assetId}/checkouts")
  public Page<EquipmentCheckoutSummaryResponse> history(
      @PathVariable UUID organizationId,
      @PathVariable UUID assetId,
      @PageableDefault(size = 20, sort = "checkedOutAt", direction = Sort.Direction.DESC)
          Pageable p) {
    access.requireCheckoutManagement(organizationId);
    assets.require(organizationId, assetId);
    return checkouts.list(
        organizationId,
        assetId,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        SafePageables.allow(p, Set.of("checkedOutAt", "dueAt", "checkedInAt", "createdAt")));
  }

  @PostMapping("/{assetId}/maintenance")
  @ResponseStatus(HttpStatus.CREATED)
  public EquipmentMaintenanceResponse maintenance(
      @PathVariable UUID organizationId,
      @PathVariable UUID assetId,
      @Valid @RequestBody CreateMaintenanceRequest r) {
    access.requireMaintenanceManagement(organizationId);
    return maintenance.create(organizationId, assetId, access.userId(), r);
  }

  @GetMapping("/{assetId}/maintenance")
  public Page<EquipmentMaintenanceResponse> maintenanceHistory(
      @PathVariable UUID organizationId,
      @PathVariable UUID assetId,
      @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) Pageable p) {
    access.requireMaintenanceManagement(organizationId);
    assets.require(organizationId, assetId);
    return maintenance.list(
        organizationId,
        assetId,
        null,
        null,
        null,
        null,
        SafePageables.allow(p, Set.of("startedAt", "completedAt", "createdAt")));
  }
}
