package org.civicops.donations.donation.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.civicops.donations.donation.*;

public record DonationResponse(
    UUID id,
    UUID organizationId,
    UUID donorId,
    UUID campaignId,
    boolean anonymous,
    BigDecimal amount,
    LocalDate donationDate,
    DonationPaymentMethod paymentMethod,
    String inKindDescription,
    boolean restricted,
    String restrictionDescription,
    String designation,
    String referenceNumber,
    String receiptNumber,
    AcknowledgementStatus acknowledgementStatus,
    String notes,
    DonationStatus status,
    Instant reversedAt,
    UUID reversedByUserId,
    String reversalReason,
    UUID createdByUserId,
    Instant createdAt,
    Instant updatedAt) {
  public static DonationResponse from(Donation d) {
    return new DonationResponse(
        d.getId(),
        d.getOrganization().getId(),
        d.getDonor() == null ? null : d.getDonor().getId(),
        d.getCampaign() == null ? null : d.getCampaign().getId(),
        d.isAnonymous(),
        d.getAmount(),
        d.getDonationDate(),
        d.getPaymentMethod(),
        d.getInKindDescription(),
        d.isRestricted(),
        d.getRestrictionDescription(),
        d.getDesignation(),
        d.getReferenceNumber(),
        d.getReceiptNumber(),
        d.getAcknowledgementStatus(),
        d.getNotes(),
        d.getStatus(),
        d.getReversedAt(),
        d.getReversedBy() == null ? null : d.getReversedBy().getId(),
        d.getReversalReason(),
        d.getCreatedBy().getId(),
        d.getCreatedAt(),
        d.getUpdatedAt());
  }
}
