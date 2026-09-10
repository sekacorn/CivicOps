package org.civicops.equipment.checkout.dto;

import java.time.*;
import java.util.UUID;
import org.civicops.equipment.asset.EquipmentCondition;
import org.civicops.equipment.checkout.*;

public record EquipmentCheckoutDetailResponse(
    UUID id,
    UUID organizationId,
    UUID assetId,
    String assetTag,
    String assetName,
    UUID borrowerUserId,
    String borrowerName,
    String borrowerEmail,
    Instant checkedOutAt,
    Instant dueAt,
    Instant checkedInAt,
    EquipmentCondition checkoutCondition,
    EquipmentCondition returnCondition,
    CheckoutStatus status,
    boolean overdue,
    String notes,
    UUID issuedByUserId,
    UUID receivedByUserId,
    Instant createdAt,
    Instant updatedAt) {
  public static EquipmentCheckoutDetailResponse from(EquipmentCheckout c, Instant now) {
    return new EquipmentCheckoutDetailResponse(
        c.getId(),
        c.getOrganization().getId(),
        c.getEquipmentAsset().getId(),
        c.getEquipmentAsset().getAssetTag(),
        c.getEquipmentAsset().getName(),
        c.getBorrowerUser() == null ? null : c.getBorrowerUser().getId(),
        c.borrowerDisplayName(),
        c.getBorrowerEmail(),
        c.getCheckedOutAt(),
        c.getDueAt(),
        c.getCheckedInAt(),
        c.getCheckoutCondition(),
        c.getReturnCondition(),
        c.getStatus(),
        c.isOverdue(now),
        c.getNotes(),
        c.getIssuedBy().getId(),
        c.getReceivedBy() == null ? null : c.getReceivedBy().getId(),
        c.getCreatedAt(),
        c.getUpdatedAt());
  }
}
