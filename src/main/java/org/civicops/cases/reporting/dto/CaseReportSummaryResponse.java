package org.civicops.cases.reporting.dto;

import java.time.LocalDate;
import java.util.UUID;

public record CaseReportSummaryResponse(
    UUID organizationId,
    LocalDate from,
    LocalDate to,
    long activeClients,
    long openCases,
    long inProgressCases,
    long onHoldCases,
    long casesClosedInPeriod,
    long overdueTasks) {}
