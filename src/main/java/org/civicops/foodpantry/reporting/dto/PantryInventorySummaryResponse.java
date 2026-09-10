package org.civicops.foodpantry.reporting.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PantryInventorySummaryResponse(
    UUID organizationId,
    UUID pantryId,
    long totalItemTypes,
    BigDecimal totalAvailableQuantity,
    long lowStockItemCount,
    long expiredLotCount,
    long expiringSoonLotCount) {}
