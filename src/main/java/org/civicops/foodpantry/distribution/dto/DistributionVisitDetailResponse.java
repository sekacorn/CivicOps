package org.civicops.foodpantry.distribution.dto;

import java.time.Instant;
import java.util.*;
import org.civicops.foodpantry.distribution.*;

public record DistributionVisitDetailResponse(
    UUID id,
    UUID organizationId,
    UUID pantryId,
    String pantryName,
    UUID householdId,
    String recipientDisplayName,
    Instant visitDateTime,
    int householdSizeAtVisit,
    String notes,
    UUID servedByUserId,
    DistributionVisitStatus status,
    Instant completedAt,
    Instant cancelledAt,
    List<DistributionItemResponse> items,
    Instant createdAt,
    Instant updatedAt,
    long version) {
  public static DistributionVisitDetailResponse from(
      PantryDistributionVisit v, List<PantryDistributionItem> items) {
    return new DistributionVisitDetailResponse(
        v.getId(),
        v.getOrganization().getId(),
        v.getPantryLocation().getId(),
        v.getPantryLocation().getName(),
        v.getHousehold() == null ? null : v.getHousehold().getId(),
        v.getHousehold() == null ? v.getRecipientName() : v.getHousehold().displayName(),
        v.getVisitDateTime(),
        v.getHouseholdSizeAtVisit(),
        v.getNotes(),
        v.getServedBy().getId(),
        v.getStatus(),
        v.getCompletedAt(),
        v.getCancelledAt(),
        items.stream().map(DistributionItemResponse::from).toList(),
        v.getCreatedAt(),
        v.getUpdatedAt(),
        v.getVersion());
  }
}
