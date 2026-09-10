package org.civicops.foodpantry.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;
import org.civicops.foodpantry.item.*;

public record InventoryAvailabilityResponse(
    UUID pantryId,
    UUID itemId,
    String itemName,
    FoodCategory category,
    UnitType unitType,
    BigDecimal availableQuantity,
    BigDecimal expiringSoonQuantity,
    BigDecimal expiredQuantity,
    BigDecimal reorderThreshold,
    boolean lowStock) {}
