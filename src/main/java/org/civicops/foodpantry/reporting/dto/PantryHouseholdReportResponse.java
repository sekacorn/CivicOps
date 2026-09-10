package org.civicops.foodpantry.reporting.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PantryHouseholdReportResponse(
    UUID organizationId,
    LocalDate from,
    LocalDate to,
    long activeHouseholds,
    long householdVisits,
    long householdsServedOnce,
    long householdsServedMultipleTimes) {}
