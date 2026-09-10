package org.civicops.grants.reporting.dto;

import java.math.BigDecimal;

public record OrganizationGrantSummaryResponse(
    long totalGrants,
    long activeGrants,
    BigDecimal totalAwarded,
    BigDecimal totalSpent,
    BigDecimal remainingBalance,
    BigDecimal averageUtilizationPercent,
    long reportsDueSoon) {}
