package org.civicops.scholarships.review;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.civicops.core.organization.Organization;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "scholarship_review")
public class ScholarshipReview extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "assignment_id", nullable = false, unique = true)
  private ScholarshipReviewAssignment assignment;

  @Column(nullable = false, precision = 5, scale = 2)
  private BigDecimal score;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ReviewRecommendation recommendation;

  @Column(columnDefinition = "TEXT")
  private String comments;

  @Column(nullable = false)
  private Instant submittedAt;

  protected ScholarshipReview() {}

  public ScholarshipReview(
      ScholarshipReviewAssignment a,
      BigDecimal score,
      ReviewRecommendation recommendation,
      String comments,
      Instant at) {
    organization = a.getOrganization();
    assignment = a;
    this.score = score;
    this.recommendation = recommendation;
    this.comments = comments;
    submittedAt = at;
    if (score.signum() < 0 || score.compareTo(BigDecimal.valueOf(100)) > 0)
      throw new BusinessRuleException(
          "INVALID_REVIEW_SCORE", "Review score must be between 0.00 and 100.00");
  }

  public ScholarshipReviewAssignment getAssignment() {
    return assignment;
  }

  public BigDecimal getScore() {
    return score;
  }

  public ReviewRecommendation getRecommendation() {
    return recommendation;
  }

  public String getComments() {
    return comments;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }
}
