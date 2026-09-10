package org.civicops.cases;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.civicops.cases.casefile.*;
import org.civicops.cases.security.CaseAccessService;
import org.civicops.core.membership.*;
import org.civicops.core.security.*;
import org.civicops.core.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class CaseAccessServiceTest {
  private final CurrentUserProvider current = mock(CurrentUserProvider.class);
  private final OrganizationAccessService orgAccess = mock(OrganizationAccessService.class);
  private final CaseRecordRepository cases = mock(CaseRecordRepository.class);
  private final CaseAccessService access = new CaseAccessService(current, orgAccess, cases);
  private final UUID org = UUID.randomUUID(), worker = UUID.randomUUID();

  @Test
  void assignedWorkerCanReadButOtherWorkerCannot() {
    CaseRecord record = mock(CaseRecord.class);
    User assigned = mock(User.class);
    OrganizationMembership membership = mock(OrganizationMembership.class);
    when(current.currentUserId()).thenReturn(worker);
    when(orgAccess.requireAnyRole(worker, org, Role.CASE_MANAGER, Role.CASE_WORKER))
        .thenReturn(membership);
    when(membership.getRole()).thenReturn(Role.CASE_WORKER);
    when(record.getAssignedUser()).thenReturn(assigned);
    when(assigned.getId()).thenReturn(worker);
    assertThatCode(() -> access.requireAssignedCaseAccess(org, record)).doesNotThrowAnyException();
    when(assigned.getId()).thenReturn(UUID.randomUUID());
    assertThatThrownBy(() -> access.requireAssignedCaseAccess(org, record))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void managerHasOrganizationWideCaseAccess() {
    CaseRecord record = mock(CaseRecord.class);
    OrganizationMembership membership = mock(OrganizationMembership.class);
    when(current.currentUserId()).thenReturn(worker);
    when(orgAccess.requireAnyRole(worker, org, Role.CASE_MANAGER, Role.CASE_WORKER))
        .thenReturn(membership);
    when(membership.getRole()).thenReturn(Role.CASE_MANAGER);
    assertThatCode(() -> access.requireSensitiveCaseAccess(org, record)).doesNotThrowAnyException();
  }

  @Test
  void programManagerGetsAggregateReportingButNotSensitiveDetail() {
    when(current.currentUserId()).thenReturn(worker);
    access.requireReporting(org);
    verify(orgAccess).requireAnyRole(worker, org, Role.CASE_MANAGER, Role.PROGRAM_MANAGER);
    CaseRecord record = mock(CaseRecord.class);
    doThrow(new AccessDeniedException("denied"))
        .when(orgAccess)
        .requireAnyRole(worker, org, Role.CASE_MANAGER, Role.CASE_WORKER);
    assertThatThrownBy(() -> access.requireSensitiveCaseAccess(org, record))
        .isInstanceOf(AccessDeniedException.class);
  }
}
