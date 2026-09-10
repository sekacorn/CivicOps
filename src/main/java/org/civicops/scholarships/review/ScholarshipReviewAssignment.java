package org.civicops.scholarships.review;

import jakarta.persistence.*;
import java.time.Instant;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.scholarships.application.ScholarshipApplication;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "scholarship_review_assignment")
public class ScholarshipReviewAssignment extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "application_id", nullable = false)
  private ScholarshipApplication application;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reviewer_user_id", nullable = false)
  private User reviewer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "assigned_by_user_id", nullable = false)
  private User assignedBy;

  @Column(nullable = false)
  private Instant assignedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ReviewAssignmentStatus status = ReviewAssignmentStatus.ASSIGNED;

  private Instant completedAt;

  protected ScholarshipReviewAssignment() {}

  public ScholarshipReviewAssignment(ScholarshipApplication a, User reviewer, User by, Instant at) {
    organization = a.getOrganization();
    application = a;
    this.reviewer = reviewer;
    assignedBy = by;
    assignedAt = at;
  }

  public void start() {
    if (status != ReviewAssignmentStatus.ASSIGNED) invalid();
    status = ReviewAssignmentStatus.IN_PROGRESS;
  }

  public void complete(Instant at) {
    if (status != ReviewAssignmentStatus.ASSIGNED && status != ReviewAssignmentStatus.IN_PROGRESS)
      invalid();
    status = ReviewAssignmentStatus.COMPLETED;
    completedAt = at;
  }

  public void cancel() {
    if (status == ReviewAssignmentStatus.COMPLETED || status == ReviewAssignmentStatus.CANCELLED)
      invalid();
    status = ReviewAssignmentStatus.CANCELLED;
  }

  private void invalid() {
    throw new BusinessRuleException(
        "INVALID_REVIEW_ASSIGNMENT_TRANSITION",
        "Review assignment cannot transition from " + status);
  }

  public Organization getOrganization() {
    return organization;
  }

  public ScholarshipApplication getApplication() {
    return application;
  }

  public User getReviewer() {
    return reviewer;
  }

  public User getAssignedBy() {
    return assignedBy;
  }

  public Instant getAssignedAt() {
    return assignedAt;
  }

  public ReviewAssignmentStatus getStatus() {
    return status;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }
}
