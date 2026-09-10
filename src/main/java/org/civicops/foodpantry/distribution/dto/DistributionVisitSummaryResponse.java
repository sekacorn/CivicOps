package org.civicops.foodpantry.distribution.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.foodpantry.distribution.*;

public record DistributionVisitSummaryResponse(
    UUID id,
    UUID pantryId,
    UUID householdId,
    String recipientDisplayName,
    Instant visitDateTime,
    int householdSizeAtVisit,
    DistributionVisitStatus status,
    Instant completedAt) {
  public static DistributionVisitSummaryResponse from(PantryDistributionVisit v) {
    return new DistributionVisitSummaryResponse(
        v.getId(),
        v.getPantryLocation().getId(),
        v.getHousehold() == null ? null : v.getHousehold().getId(),
        v.getHousehold() == null ? v.getRecipientName() : v.getHousehold().displayName(),
        v.getVisitDateTime(),
        v.getHouseholdSizeAtVisit(),
        v.getStatus(),
        v.getCompletedAt());
  }
}
