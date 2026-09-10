package org.civicops.board;

import static org.mockito.Mockito.*;

import java.util.*;
import org.civicops.board.security.BoardAccessService;
import org.civicops.core.membership.Role;
import org.civicops.core.security.*;
import org.junit.jupiter.api.Test;

class BoardAccessServiceTest {
  CurrentUserProvider current = mock(CurrentUserProvider.class);
  OrganizationAccessService organizations = mock(OrganizationAccessService.class);
  BoardAccessService access = new BoardAccessService(current, organizations);
  UUID user = UUID.randomUUID(), org = UUID.randomUUID();

  BoardAccessServiceTest() {
    when(current.currentUserId()).thenReturn(user);
  }

  @Test
  void managementUsesBoardManagerBoundary() {
    access.requireBoardManagement(org);
    verify(organizations).requireAnyRole(user, org, Role.BOARD_MANAGER);
  }

  @Test
  void readAllowsManagersAndBoardMembers() {
    access.requireBoardRead(org);
    verify(organizations).requireAnyRole(user, org, Role.BOARD_MANAGER, Role.BOARD_MEMBER);
  }

  @Test
  void selfAndVotingRequireBoardMember() {
    access.requireBoardMemberSelfAccess(org);
    access.requireVotingAccess(org);
    verify(organizations, times(2)).requireAnyRole(user, org, Role.BOARD_MEMBER);
  }

  @Test
  void minutesManagementUsesManager() {
    access.requireMinutesManagement(org);
    verify(organizations).requireAnyRole(user, org, Role.BOARD_MANAGER);
  }

  @Test
  void aggregateReportingAllowsProgramManager() {
    access.requireGovernanceReporting(org);
    verify(organizations).requireAnyRole(user, org, Role.BOARD_MANAGER, Role.PROGRAM_MANAGER);
  }
}
