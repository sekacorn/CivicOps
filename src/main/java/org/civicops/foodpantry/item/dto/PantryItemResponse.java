package org.civicops.foodpantry.item.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.civicops.foodpantry.item.*;

public record PantryItemResponse(
    UUID id,
    UUID organizationId,
    String sku,
    String name,
    String description,
    FoodCategory category,
    UnitType unitType,
    boolean active,
    boolean trackExpiration,
    BigDecimal reorderThreshold,
    String notes,
    Instant createdAt,
    Instant updatedAt,
    long version) {
  public static PantryItemResponse from(PantryItem i) {
    return new PantryItemResponse(
        i.getId(),
        i.getOrganization().getId(),
        i.getSku(),
        i.getName(),
        i.getDescription(),
        i.getCategory(),
        i.getUnitType(),
        i.isActive(),
        i.isTrackExpiration(),
        i.getReorderThreshold(),
        i.getNotes(),
        i.getCreatedAt(),
        i.getUpdatedAt(),
        i.getVersion());
  }
}
