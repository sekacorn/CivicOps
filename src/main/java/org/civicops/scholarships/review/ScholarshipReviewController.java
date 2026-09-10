package org.civicops.scholarships.review;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.civicops.scholarships.application.dto.ReviewerApplicationResponse;
import org.civicops.scholarships.review.dto.*;
import org.civicops.scholarships.security.ScholarshipAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
@Tag(name = "Scholarship Reviews")
public class ScholarshipReviewController {
  private final ScholarshipReviewService reviews;
  private final ScholarshipAccessService access;

  public ScholarshipReviewController(ScholarshipReviewService r, ScholarshipAccessService a) {
    reviews = r;
    access = a;
  }

  @GetMapping("/scholarship-reviews/me")
  public Page<ReviewAssignmentResponse> mine(
      @PathVariable UUID organizationId,
      @PageableDefault(size = 20, sort = "assignedAt") Pageable p) {
    access.requireReviewerAccess(organizationId);
    return reviews.mine(
        organizationId,
        access.userId(),
        SafePageables.allow(p, Set.of("assignedAt", "status", "completedAt")));
  }

  @GetMapping("/scholarship-review-assignments/{assignmentId}/application")
  public ReviewerApplicationResponse application(
      @PathVariable UUID organizationId, @PathVariable UUID assignmentId) {
    ScholarshipReviewAssignment assignment = reviews.require(organizationId, assignmentId);
    access.requireOwnAssignment(organizationId, assignment);
    return reviews.application(organizationId, assignmentId);
  }

  @PostMapping("/scholarship-review-assignments/{assignmentId}/start")
  public ReviewAssignmentResponse start(
      @PathVariable UUID organizationId, @PathVariable UUID assignmentId) {
    ScholarshipReviewAssignment assignment = reviews.require(organizationId, assignmentId);
    access.requireOwnAssignment(organizationId, assignment);
    return reviews.start(organizationId, assignmentId);
  }

  @PostMapping("/scholarship-review-assignments/{assignmentId}/submit-review")
  public ScholarshipReviewResponse submit(
      @PathVariable UUID organizationId,
      @PathVariable UUID assignmentId,
      @Valid @RequestBody SubmitReviewRequest r) {
    ScholarshipReviewAssignment assignment = reviews.require(organizationId, assignmentId);
    access.requireOwnAssignment(organizationId, assignment);
    return reviews.submit(organizationId, assignmentId, r);
  }
}
