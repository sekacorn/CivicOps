package org.civicops.scholarships.application;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.civicops.scholarships.application.dto.*;
import org.civicops.scholarships.review.*;
import org.civicops.scholarships.review.dto.*;
import org.civicops.scholarships.security.ScholarshipAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/scholarship-applications")
@Tag(name = "Scholarship Applications")
public class ScholarshipApplicationController {
  private final ScholarshipApplicationService applications;
  private final ScholarshipDocumentService documents;
  private final ScholarshipReviewService reviews;
  private final ScholarshipAccessService access;

  public ScholarshipApplicationController(
      ScholarshipApplicationService a,
      ScholarshipDocumentService d,
      ScholarshipReviewService r,
      ScholarshipAccessService x) {
    applications = a;
    documents = d;
    reviews = r;
    access = x;
  }

  @GetMapping("/{applicationId}")
  public ScholarshipApplicationDetailResponse detail(
      @PathVariable UUID organizationId, @PathVariable UUID applicationId) {
    access.requireApplicantDetailAccess(organizationId);
    return applications.detail(organizationId, applicationId);
  }

  @PatchMapping("/{applicationId}")
  public ScholarshipApplicationDetailResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID applicationId,
      @Valid @RequestBody UpdateScholarshipApplicationRequest r) {
    access.requireScholarshipManagement(organizationId);
    return applications.update(organizationId, applicationId, r);
  }

  @PostMapping("/{applicationId}/submit")
  public ScholarshipApplicationDetailResponse submit(
      @PathVariable UUID organizationId, @PathVariable UUID applicationId) {
    access.requireScholarshipManagement(organizationId);
    return applications.submit(organizationId, applicationId);
  }

  @PostMapping("/{applicationId}/withdraw")
  public ScholarshipApplicationDetailResponse withdraw(
      @PathVariable UUID organizationId, @PathVariable UUID applicationId) {
    access.requireScholarshipManagement(organizationId);
    return applications.withdraw(organizationId, applicationId);
  }

  @PostMapping("/{applicationId}/finalist")
  public ScholarshipApplicationDetailResponse finalist(
      @PathVariable UUID organizationId, @PathVariable UUID applicationId) {
    access.requireScholarshipManagement(organizationId);
    return applications.finalist(organizationId, applicationId);
  }

  @PostMapping("/{applicationId}/select")
  public ScholarshipApplicationDetailResponse select(
      @PathVariable UUID organizationId, @PathVariable UUID applicationId) {
    access.requireScholarshipManagement(organizationId);
    return applications.select(organizationId, applicationId);
  }

  @PostMapping("/{applicationId}/not-select")
  public ScholarshipApplicationDetailResponse notSelect(
      @PathVariable UUID organizationId, @PathVariable UUID applicationId) {
    access.requireScholarshipManagement(organizationId);
    return applications.notSelect(organizationId, applicationId);
  }

  @PostMapping("/{applicationId}/reviewers")
  @ResponseStatus(HttpStatus.CREATED)
  public ReviewAssignmentResponse reviewer(
      @PathVariable UUID organizationId,
      @PathVariable UUID applicationId,
      @Valid @RequestBody AssignReviewerRequest r) {
    access.requireScholarshipManagement(organizationId);
    return reviews.assign(organizationId, applicationId, r.reviewerUserId(), access.userId());
  }

  @GetMapping("/{applicationId}/reviewers")
  public List<ReviewAssignmentResponse> reviewers(
      @PathVariable UUID organizationId, @PathVariable UUID applicationId) {
    access.requireScholarshipManagement(organizationId);
    return reviews.forApplication(organizationId, applicationId);
  }

  @GetMapping("/{applicationId}/reviews")
  public List<ScholarshipReviewResponse> reviewResults(
      @PathVariable UUID organizationId, @PathVariable UUID applicationId) {
    access.requireScholarshipManagement(organizationId);
    return reviews.reviewsForApplication(organizationId, applicationId);
  }

  @PostMapping("/{applicationId}/documents")
  @ResponseStatus(HttpStatus.CREATED)
  public ScholarshipDocumentResponse document(
      @PathVariable UUID organizationId,
      @PathVariable UUID applicationId,
      @Valid @RequestBody CreateScholarshipDocumentRequest r) {
    access.requireScholarshipManagement(organizationId);
    return documents.add(organizationId, applicationId, r);
  }

  @GetMapping("/{applicationId}/documents")
  public List<ScholarshipDocumentResponse> documents(
      @PathVariable UUID organizationId, @PathVariable UUID applicationId) {
    access.requireScholarshipManagement(organizationId);
    return documents.list(organizationId, applicationId);
  }

  @PostMapping("/{applicationId}/documents/{documentId}/verify")
  public ScholarshipDocumentResponse verify(
      @PathVariable UUID organizationId,
      @PathVariable UUID applicationId,
      @PathVariable UUID documentId) {
    access.requireScholarshipManagement(organizationId);
    return documents.verify(organizationId, applicationId, documentId, access.userId());
  }
}
