package org.civicops.equipment.asset.dto;

import java.util.UUID;
import org.civicops.equipment.asset.*;

public record EquipmentAssetSummaryResponse(
    UUID id,
    String assetTag,
    String name,
    UUID categoryId,
    String categoryName,
    EquipmentCondition condition,
    AssetStatus status,
    String location) {
  public static EquipmentAssetSummaryResponse from(EquipmentAsset a) {
    return new EquipmentAssetSummaryResponse(
        a.getId(),
        a.getAssetTag(),
        a.getName(),
        a.getCategory() == null ? null : a.getCategory().getId(),
        a.getCategory() == null ? null : a.getCategory().getName(),
        a.getCondition(),
        a.getStatus(),
        a.getLocation());
  }
}
