package org.civicops.foodpantry.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PantryWasteReportResponse(
    UUID organizationId,
    UUID pantryId,
    LocalDate from,
    LocalDate to,
    BigDecimal expiredQuantity,
    BigDecimal spoiledQuantity) {}
