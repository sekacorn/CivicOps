package org.civicops.donations.donor.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.donations.donor.*;

public record DonorSummaryResponse(
    UUID id,
    DonorType donorType,
    String displayName,
    boolean anonymous,
    boolean communicationOptOut,
    Instant createdAt,
    Instant updatedAt) {
  public static DonorSummaryResponse from(Donor d) {
    return new DonorSummaryResponse(
        d.getId(),
        d.getDonorType(),
        d.displayName(),
        d.isAnonymous(),
        d.isCommunicationOptOut(),
        d.getCreatedAt(),
        d.getUpdatedAt());
  }
}
