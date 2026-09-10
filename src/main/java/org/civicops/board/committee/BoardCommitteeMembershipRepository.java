package org.civicops.board.committee;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardCommitteeMembershipRepository
    extends JpaRepository<BoardCommitteeMembership, UUID> {
  Optional<BoardCommitteeMembership> findByIdAndOrganizationId(UUID id, UUID org);

  boolean existsByCommitteeIdAndBoardMemberIdAndActiveTrue(UUID committee, UUID member);

  List<BoardCommitteeMembership> findAllByOrganizationIdAndCommitteeIdOrderByStartDate(
      UUID org, UUID committee);
}
