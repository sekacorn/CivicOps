package org.civicops.foodpantry.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public record PantryDistributionReportResponse(
    UUID organizationId,
    UUID pantryId,
    LocalDate from,
    LocalDate to,
    long visitsCompleted,
    long householdsServed,
    long uniqueHouseholdsServed,
    BigDecimal totalQuantityDistributed,
    BigDecimal averageHouseholdSize,
    List<CategoryDistributionQuantity> quantityByCategory) {}
