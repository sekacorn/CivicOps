package org.civicops.foodpantry.distribution.dto;

import java.math.BigDecimal;
import java.util.UUID;
import org.civicops.foodpantry.distribution.PantryDistributionItem;

public record DistributionItemResponse(UUID id, UUID itemId, String itemName, BigDecimal quantity) {
  public static DistributionItemResponse from(PantryDistributionItem i) {
    return new DistributionItemResponse(
        i.getId(), i.getPantryItem().getId(), i.getPantryItem().getName(), i.getQuantity());
  }
}
