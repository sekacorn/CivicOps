package org.civicops.scholarships.review.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.scholarships.review.*;

public record ReviewAssignmentResponse(
    UUID id,
    UUID applicationId,
    UUID reviewerUserId,
    String reviewerName,
    ReviewAssignmentStatus status,
    Instant assignedAt,
    Instant completedAt) {
  public static ReviewAssignmentResponse from(ScholarshipReviewAssignment a) {
    return new ReviewAssignmentResponse(
        a.getId(),
        a.getApplication().getId(),
        a.getReviewer().getId(),
        a.getReviewer().getFirstName() + " " + a.getReviewer().getLastName(),
        a.getStatus(),
        a.getAssignedAt(),
        a.getCompletedAt());
  }
}
