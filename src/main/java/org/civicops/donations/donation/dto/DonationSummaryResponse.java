package org.civicops.donations.donation.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.civicops.donations.donation.*;

public record DonationSummaryResponse(
    UUID id,
    UUID donorId,
    UUID campaignId,
    boolean anonymous,
    BigDecimal amount,
    LocalDate donationDate,
    DonationPaymentMethod paymentMethod,
    boolean restricted,
    String designation,
    AcknowledgementStatus acknowledgementStatus,
    DonationStatus status,
    Instant createdAt) {
  public static DonationSummaryResponse from(Donation d) {
    return new DonationSummaryResponse(
        d.getId(),
        d.getDonor() == null ? null : d.getDonor().getId(),
        d.getCampaign() == null ? null : d.getCampaign().getId(),
        d.isAnonymous(),
        d.getAmount(),
        d.getDonationDate(),
        d.getPaymentMethod(),
        d.isRestricted(),
        d.getDesignation(),
        d.getAcknowledgementStatus(),
        d.getStatus(),
        d.getCreatedAt());
  }
}
