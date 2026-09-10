package org.civicops.equipment.category.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.equipment.category.EquipmentCategory;

public record EquipmentCategoryResponse(
    UUID id,
    String name,
    String description,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {
  public static EquipmentCategoryResponse from(EquipmentCategory c) {
    return new EquipmentCategoryResponse(
        c.getId(),
        c.getName(),
        c.getDescription(),
        c.isActive(),
        c.getCreatedAt(),
        c.getUpdatedAt());
  }
}
