package org.civicops.equipment.checkout.dto;

import java.time.*;
import java.util.UUID;
import org.civicops.equipment.checkout.*;

public record EquipmentCheckoutSummaryResponse(
    UUID id,
    UUID assetId,
    String assetTag,
    String assetName,
    CheckoutStatus status,
    Instant checkedOutAt,
    Instant dueAt,
    Instant checkedInAt,
    boolean overdue) {
  public static EquipmentCheckoutSummaryResponse from(EquipmentCheckout c, Instant now) {
    return new EquipmentCheckoutSummaryResponse(
        c.getId(),
        c.getEquipmentAsset().getId(),
        c.getEquipmentAsset().getAssetTag(),
        c.getEquipmentAsset().getName(),
        c.getStatus(),
        c.getCheckedOutAt(),
        c.getDueAt(),
        c.getCheckedInAt(),
        c.isOverdue(now));
  }
}
