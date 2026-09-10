package org.civicops.donations.reporting.dto;

import java.math.BigDecimal;
import org.civicops.donations.donation.DonationPaymentMethod;

public record PaymentMethodTotalResponse(
    DonationPaymentMethod paymentMethod, BigDecimal amount, long donationCount) {}
