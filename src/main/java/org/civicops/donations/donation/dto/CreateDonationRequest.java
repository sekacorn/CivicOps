package org.civicops.donations.donation.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.civicops.donations.donation.*;

public record CreateDonationRequest(
    UUID donorId,
    boolean anonymous,
    @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 17, fraction = 2)
        BigDecimal amount,
    @NotNull LocalDate donationDate,
    @NotNull DonationPaymentMethod paymentMethod,
    @Size(max = 500) String inKindDescription,
    UUID campaignId,
    boolean restricted,
    String restrictionDescription,
    @Size(max = 200) String designation,
    @Size(max = 100) String referenceNumber,
    @Size(max = 100) String receiptNumber,
    AcknowledgementStatus acknowledgementStatus,
    String notes) {}
