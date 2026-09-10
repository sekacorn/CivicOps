package org.civicops.scholarships.reporting.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ScholarshipProgramReportResponse(
    UUID programId,
    String programName,
    long applications,
    long submitted,
    long eligible,
    long underReview,
    long finalists,
    long selected,
    long notSelected,
    long awardCount,
    BigDecimal totalAwardAmount) {}
