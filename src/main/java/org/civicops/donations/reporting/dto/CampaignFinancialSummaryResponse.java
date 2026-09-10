package org.civicops.donations.reporting.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CampaignFinancialSummaryResponse(
    UUID campaignId,
    String campaignName,
    BigDecimal goalAmount,
    BigDecimal amountRaised,
    BigDecimal remainingToGoal,
    BigDecimal percentageOfGoal,
    long donationCount) {}
