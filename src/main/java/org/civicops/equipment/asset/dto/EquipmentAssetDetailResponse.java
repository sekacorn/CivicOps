package org.civicops.equipment.asset.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.civicops.equipment.asset.*;

public record EquipmentAssetDetailResponse(
    UUID id,
    UUID organizationId,
    String assetTag,
    String name,
    String description,
    UUID categoryId,
    String categoryName,
    String manufacturer,
    String model,
    String serialNumber,
    LocalDate purchaseDate,
    BigDecimal purchaseValue,
    EquipmentCondition condition,
    AssetStatus status,
    String location,
    String notes,
    Instant createdAt,
    Instant updatedAt,
    long version) {
  public static EquipmentAssetDetailResponse from(EquipmentAsset a) {
    return new EquipmentAssetDetailResponse(
        a.getId(),
        a.getOrganization().getId(),
        a.getAssetTag(),
        a.getName(),
        a.getDescription(),
        a.getCategory() == null ? null : a.getCategory().getId(),
        a.getCategory() == null ? null : a.getCategory().getName(),
        a.getManufacturer(),
        a.getModel(),
        a.getSerialNumber(),
        a.getPurchaseDate(),
        a.getPurchaseValue(),
        a.getCondition(),
        a.getStatus(),
        a.getLocation(),
        a.getNotes(),
        a.getCreatedAt(),
        a.getUpdatedAt(),
        a.getVersion());
  }
}
