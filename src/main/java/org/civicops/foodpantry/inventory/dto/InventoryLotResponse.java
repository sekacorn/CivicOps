package org.civicops.foodpantry.inventory.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.civicops.foodpantry.inventory.*;

public record InventoryLotResponse(
    UUID id,
    UUID pantryId,
    String pantryName,
    UUID itemId,
    String itemName,
    String lotNumber,
    LocalDate receivedDate,
    LocalDate expirationDate,
    BigDecimal quantityReceived,
    BigDecimal quantityRemaining,
    InventorySourceType sourceType,
    String sourceReference,
    String notes,
    Instant createdAt,
    Instant updatedAt,
    long version) {
  public static InventoryLotResponse from(PantryInventoryLot l) {
    return new InventoryLotResponse(
        l.getId(),
        l.getPantryLocation().getId(),
        l.getPantryLocation().getName(),
        l.getPantryItem().getId(),
        l.getPantryItem().getName(),
        l.getLotNumber(),
        l.getReceivedDate(),
        l.getExpirationDate(),
        l.getQuantityReceived(),
        l.getQuantityRemaining(),
        l.getSourceType(),
        l.getSourceReference(),
        l.getNotes(),
        l.getCreatedAt(),
        l.getUpdatedAt(),
        l.getVersion());
  }
}
