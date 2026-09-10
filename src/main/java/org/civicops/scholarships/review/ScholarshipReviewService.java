package org.civicops.scholarships.review;

import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import org.civicops.core.membership.*;
import org.civicops.core.user.*;
import org.civicops.scholarships.application.*;
import org.civicops.scholarships.application.dto.ReviewerApplicationResponse;
import org.civicops.scholarships.program.ScholarshipProgramStatus;
import org.civicops.scholarships.review.dto.*;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScholarshipReviewService {
  private final ScholarshipReviewAssignmentRepository assignments;
  private final ScholarshipReviewRepository reviews;
  private final ScholarshipApplicationService applications;
  private final OrganizationMembershipRepository memberships;
  private final UserService users;
  private final Clock clock;

  public ScholarshipReviewService(
      ScholarshipReviewAssignmentRepository a,
      ScholarshipReviewRepository r,
      ScholarshipApplicationService apps,
      OrganizationMembershipRepository m,
      UserService u,
      Clock c) {
    assignments = a;
    reviews = r;
    applications = apps;
    memberships = m;
    users = u;
    clock = c;
  }

  @Transactional
  public ReviewAssignmentResponse assign(
      UUID org, UUID applicationId, UUID reviewerId, UUID actor) {
    ScholarshipApplication app = applications.require(org, applicationId);
    if (app.getProgram().getStatus() != ScholarshipProgramStatus.REVIEWING)
      throw new BusinessRuleException(
          "PROGRAM_NOT_REVIEWING", "Reviewers can be assigned only while the program is REVIEWING");
    OrganizationMembership membership =
        memberships
            .findByOrganizationIdAndUserIdAndActiveTrue(org, reviewerId)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "INVALID_SCHOLARSHIP_REVIEWER",
                        "Reviewer must be an active organization member"));
    if (membership.getRole() != Role.SCHOLARSHIP_REVIEWER
        && membership.getRole() != Role.SCHOLARSHIP_MANAGER
        && membership.getRole() != Role.ORG_ADMIN)
      throw new BusinessRuleException(
          "INVALID_SCHOLARSHIP_REVIEWER_ROLE", "Reviewer must have a scholarship review role");
    if (assignments.existsByApplicationIdAndReviewerId(applicationId, reviewerId))
      throw new ConflictException(
          "DUPLICATE_REVIEW_ASSIGNMENT", "Reviewer is already assigned to this application");
    if (app.getStatus() == ScholarshipApplicationStatus.SUBMITTED) app.startReview();
    else if (app.getStatus() != ScholarshipApplicationStatus.UNDER_REVIEW
        && app.getStatus() != ScholarshipApplicationStatus.FINALIST)
      throw new BusinessRuleException(
          "APPLICATION_NOT_REVIEWABLE", "Application is not eligible for review assignment");
    return ReviewAssignmentResponse.from(
        assignments.save(
            new ScholarshipReviewAssignment(
                app,
                users.requireEntity(reviewerId),
                users.requireEntity(actor),
                Instant.now(clock))));
  }

  @Transactional
  public ReviewAssignmentResponse start(UUID org, UUID id) {
    ScholarshipReviewAssignment a = require(org, id);
    a.start();
    return ReviewAssignmentResponse.from(a);
  }

  @Transactional
  public ScholarshipReviewResponse submit(UUID org, UUID id, SubmitReviewRequest r) {
    ScholarshipReviewAssignment a = require(org, id);
    if (reviews.existsByAssignmentId(id))
      throw new ConflictException(
          "REVIEW_ALREADY_SUBMITTED", "A review has already been submitted for this assignment");
    ScholarshipReview review =
        reviews.save(
            new ScholarshipReview(
                a,
                r.score().setScale(2, RoundingMode.HALF_UP),
                r.recommendation(),
                clean(r.comments()),
                Instant.now(clock)));
    a.complete(Instant.now(clock));
    return ScholarshipReviewResponse.from(review);
  }

  @Transactional(readOnly = true)
  public ScholarshipReviewAssignment require(UUID org, UUID id) {
    return assignments
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Scholarship review assignment", id));
  }

  @Transactional(readOnly = true)
  public ReviewerApplicationResponse application(UUID org, UUID id) {
    return ReviewerApplicationResponse.from(require(org, id).getApplication());
  }

  @Transactional(readOnly = true)
  public Page<ReviewAssignmentResponse> mine(UUID org, UUID reviewer, Pageable pageable) {
    return assignments
        .findAllByOrganizationIdAndReviewerId(org, reviewer, pageable)
        .map(ReviewAssignmentResponse::from);
  }

  @Transactional(readOnly = true)
  public List<ReviewAssignmentResponse> forApplication(UUID org, UUID application) {
    applications.require(org, application);
    return assignments
        .findAll(
            (root, q, c) ->
                c.and(
                    c.equal(root.get("organization").get("id"), org),
                    c.equal(root.get("application").get("id"), application)))
        .stream()
        .map(ReviewAssignmentResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ScholarshipReviewResponse> reviewsForApplication(UUID org, UUID application) {
    applications.require(org, application);
    return reviews.findAllByAssignmentApplicationId(application).stream()
        .map(ScholarshipReviewResponse::from)
        .toList();
  }

  private static String clean(String x) {
    return x == null || x.isBlank() ? null : x.trim();
  }
}
