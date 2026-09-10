package org.civicops.scholarships.security;

import java.util.UUID;
import org.civicops.core.membership.Role;
import org.civicops.core.security.*;
import org.civicops.scholarships.review.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ScholarshipAccessService {
  private final CurrentUserProvider current;
  private final OrganizationAccessService organizations;
  private final ScholarshipReviewAssignmentRepository assignments;

  public ScholarshipAccessService(
      CurrentUserProvider c, OrganizationAccessService o, ScholarshipReviewAssignmentRepository a) {
    current = c;
    organizations = o;
    assignments = a;
  }

  public UUID userId() {
    return current.currentUserId();
  }

  public void requireScholarshipManagement(UUID org) {
    organizations.requireAnyRole(userId(), org, Role.SCHOLARSHIP_MANAGER);
  }

  public void requireProgramRead(UUID org) {
    organizations.requireAnyRole(
        userId(),
        org,
        Role.SCHOLARSHIP_MANAGER,
        Role.SCHOLARSHIP_REVIEWER,
        Role.PROGRAM_MANAGER,
        Role.VIEWER);
  }

  public void requireReviewerAccess(UUID org) {
    organizations.requireAnyRole(
        userId(), org, Role.SCHOLARSHIP_MANAGER, Role.SCHOLARSHIP_REVIEWER);
  }

  public void requireScholarshipReporting(UUID org) {
    organizations.requireAnyRole(userId(), org, Role.SCHOLARSHIP_MANAGER, Role.PROGRAM_MANAGER);
  }

  public void requireApplicantDetailAccess(UUID org) {
    requireScholarshipManagement(org);
  }

  public void requireAssignedReview(UUID org, UUID application) {
    try {
      requireScholarshipManagement(org);
      return;
    } catch (AccessDeniedException ignored) {
    }
    organizations.requireAnyRole(userId(), org, Role.SCHOLARSHIP_REVIEWER);
    if (!assignments.existsByApplicationIdAndReviewerIdAndStatusNot(
        application, userId(), ReviewAssignmentStatus.CANCELLED))
      throw new AccessDeniedException("Reviewer access is limited to assigned applications");
  }

  public void requireOwnAssignment(UUID org, ScholarshipReviewAssignment assignment) {
    try {
      requireScholarshipManagement(org);
      return;
    } catch (AccessDeniedException ignored) {
    }
    organizations.requireAnyRole(userId(), org, Role.SCHOLARSHIP_REVIEWER);
    if (!assignment.getReviewer().getId().equals(userId()))
      throw new AccessDeniedException("Review assignment belongs to another reviewer");
  }
}
