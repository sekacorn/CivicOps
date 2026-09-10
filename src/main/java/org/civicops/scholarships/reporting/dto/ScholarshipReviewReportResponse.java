package org.civicops.scholarships.reporting.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ScholarshipReviewReportResponse(
    UUID organizationId,
    long reviewsAssigned,
    long reviewsCompleted,
    long outstandingReviews,
    BigDecimal averageScore) {}
