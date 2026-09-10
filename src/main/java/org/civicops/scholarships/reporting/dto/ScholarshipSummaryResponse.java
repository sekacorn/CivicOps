package org.civicops.scholarships.reporting.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ScholarshipSummaryResponse(
    UUID organizationId,
    long activePrograms,
    long applicationsSubmitted,
    long applicationsUnderReview,
    long finalists,
    long selectedApplicants,
    long awardsOffered,
    BigDecimal totalAwarded) {}
