package org.civicops.grants.reporting.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record GrantFinancialSummaryResponse(
    UUID grantId,
    BigDecimal awardAmount,
    BigDecimal totalSpent,
    BigDecimal remainingBalance,
    BigDecimal utilizationPercent) {}
