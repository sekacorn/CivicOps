package org.civicops.scholarships.review.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.civicops.scholarships.review.*;

public record ScholarshipReviewResponse(
    UUID id,
    UUID assignmentId,
    UUID reviewerUserId,
    BigDecimal score,
    ReviewRecommendation recommendation,
    String comments,
    Instant submittedAt) {
  public static ScholarshipReviewResponse from(ScholarshipReview r) {
    return new ScholarshipReviewResponse(
        r.getId(),
        r.getAssignment().getId(),
        r.getAssignment().getReviewer().getId(),
        r.getScore(),
        r.getRecommendation(),
        r.getComments(),
        r.getSubmittedAt());
  }
}
