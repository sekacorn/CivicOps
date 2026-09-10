package org.civicops.volunteers.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VolunteerSummaryReport(
    LocalDate from,
    LocalDate to,
    long activeVolunteers,
    BigDecimal approvedHours,
    long upcomingShifts,
    long openOpportunities,
    long openShiftCapacity) {}
