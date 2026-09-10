package org.civicops.donations.reporting.dto;

import java.math.BigDecimal;

public record DonationReportSummaryResponse(
    long totalDonations,
    BigDecimal totalAmount,
    BigDecimal averageDonation,
    BigDecimal restrictedAmount,
    BigDecimal unrestrictedAmount,
    long activeCampaigns,
    long uniqueDonors) {}
