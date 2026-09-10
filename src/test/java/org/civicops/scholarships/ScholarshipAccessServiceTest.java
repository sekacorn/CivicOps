package org.civicops.scholarships;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.civicops.core.membership.Role;
import org.civicops.core.security.*;
import org.civicops.scholarships.review.*;
import org.civicops.scholarships.security.ScholarshipAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ScholarshipAccessServiceTest {
  CurrentUserProvider current = mock(CurrentUserProvider.class);
  OrganizationAccessService organizations = mock(OrganizationAccessService.class);
  ScholarshipReviewAssignmentRepository assignments =
      mock(ScholarshipReviewAssignmentRepository.class);
  ScholarshipAccessService access =
      new ScholarshipAccessService(current, organizations, assignments);
  UUID user = UUID.randomUUID(), org = UUID.randomUUID(), application = UUID.randomUUID();

  ScholarshipAccessServiceTest() {
    when(current.currentUserId()).thenReturn(user);
  }

  @Test
  void managementUsesScholarshipManagerBoundary() {
    access.requireScholarshipManagement(org);
    verify(organizations).requireAnyRole(user, org, Role.SCHOLARSHIP_MANAGER);
  }

  @Test
  void reportingAllowsProgramManager() {
    access.requireScholarshipReporting(org);
    verify(organizations).requireAnyRole(user, org, Role.SCHOLARSHIP_MANAGER, Role.PROGRAM_MANAGER);
  }

  @Test
  void programReadIsPrivacySafeRoleSet() {
    access.requireProgramRead(org);
    verify(organizations)
        .requireAnyRole(
            user,
            org,
            Role.SCHOLARSHIP_MANAGER,
            Role.SCHOLARSHIP_REVIEWER,
            Role.PROGRAM_MANAGER,
            Role.VIEWER);
  }

  @Test
  void assignedReviewerMayAccessApplication() {
    doThrow(new AccessDeniedException("not manager"))
        .when(organizations)
        .requireAnyRole(user, org, Role.SCHOLARSHIP_MANAGER);
    when(assignments.existsByApplicationIdAndReviewerIdAndStatusNot(
            application, user, ReviewAssignmentStatus.CANCELLED))
        .thenReturn(true);
    access.requireAssignedReview(org, application);
    verify(organizations).requireAnyRole(user, org, Role.SCHOLARSHIP_REVIEWER);
  }

  @Test
  void unassignedReviewerIsDenied() {
    doThrow(new AccessDeniedException("not manager"))
        .when(organizations)
        .requireAnyRole(user, org, Role.SCHOLARSHIP_MANAGER);
    assertThatThrownBy(() -> access.requireAssignedReview(org, application))
        .isInstanceOf(AccessDeniedException.class);
  }
}
