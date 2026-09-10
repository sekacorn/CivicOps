package org.civicops.foodpantry.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.civicops.foodpantry.inventory.*;

public record InventoryTransactionResponse(
    UUID id,
    UUID lotId,
    InventoryTransactionType transactionType,
    BigDecimal quantity,
    Instant occurredAt,
    String referenceType,
    UUID referenceId,
    String notes,
    UUID createdByUserId) {
  public static InventoryTransactionResponse from(PantryInventoryTransaction t) {
    return new InventoryTransactionResponse(
        t.getId(),
        t.getInventoryLot().getId(),
        t.getTransactionType(),
        t.getQuantity(),
        t.getOccurredAt(),
        t.getReferenceType(),
        t.getReferenceId(),
        t.getNotes(),
        t.getCreatedBy().getId());
  }
}
