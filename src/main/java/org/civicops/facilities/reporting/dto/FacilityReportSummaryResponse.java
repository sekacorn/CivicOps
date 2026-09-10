package org.civicops.facilities.reporting.dto;

import java.time.LocalDate;
import java.util.UUID;

public record FacilityReportSummaryResponse(
    UUID organizationId,
    LocalDate from,
    LocalDate to,
    long activeFacilities,
    long reservableSpaces,
    long pendingReservations,
    long approvedReservations,
    long reservationsInPeriod,
    long cancelledReservations) {}
