package org.civicops.equipment.maintenance.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.civicops.equipment.maintenance.*;

public record EquipmentMaintenanceResponse(
    UUID id,
    UUID assetId,
    String assetTag,
    MaintenanceType maintenanceType,
    String description,
    Instant startedAt,
    Instant completedAt,
    BigDecimal cost,
    String vendor,
    MaintenanceStatus status,
    UUID createdByUserId,
    UUID completedByUserId,
    String notes,
    Instant createdAt) {
  public static EquipmentMaintenanceResponse from(EquipmentMaintenanceRecord r) {
    return new EquipmentMaintenanceResponse(
        r.getId(),
        r.getEquipmentAsset().getId(),
        r.getEquipmentAsset().getAssetTag(),
        r.getMaintenanceType(),
        r.getDescription(),
        r.getStartedAt(),
        r.getCompletedAt(),
        r.getCost(),
        r.getVendor(),
        r.getStatus(),
        r.getCreatedBy().getId(),
        r.getCompletedBy() == null ? null : r.getCompletedBy().getId(),
        r.getNotes(),
        r.getCreatedAt());
  }
}
