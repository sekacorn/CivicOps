package org.civicops.board.security;

import java.util.UUID;
import org.civicops.core.membership.Role;
import org.civicops.core.security.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class BoardAccessService {
  private final CurrentUserProvider current;
  private final OrganizationAccessService organizations;

  public BoardAccessService(CurrentUserProvider c, OrganizationAccessService o) {
    current = c;
    organizations = o;
  }

  public UUID userId() {
    return current.currentUserId();
  }

  public void requireBoardManagement(UUID org) {
    organizations.requireAnyRole(userId(), org, Role.BOARD_MANAGER);
  }

  public void requireBoardRead(UUID org) {
    organizations.requireAnyRole(userId(), org, Role.BOARD_MANAGER, Role.BOARD_MEMBER);
  }

  public void requireBoardMemberSelfAccess(UUID org) {
    organizations.requireAnyRole(userId(), org, Role.BOARD_MEMBER);
  }

  public void requireVotingAccess(UUID org) {
    requireBoardMemberSelfAccess(org);
  }

  public void requireMinutesManagement(UUID org) {
    requireBoardManagement(org);
  }

  public void requireGovernanceReporting(UUID org) {
    organizations.requireAnyRole(userId(), org, Role.BOARD_MANAGER, Role.PROGRAM_MANAGER);
  }

  public boolean canManage(UUID org) {
    try {
      requireBoardManagement(org);
      return true;
    } catch (AccessDeniedException e) {
      return false;
    }
  }
}
