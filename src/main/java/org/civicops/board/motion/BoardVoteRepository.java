package org.civicops.board.motion;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardVoteRepository extends JpaRepository<BoardVote, UUID> {
  boolean existsByMotionIdAndBoardMemberId(UUID motion, UUID member);

  List<BoardVote> findAllByOrganizationIdAndMotionIdOrderByCastAt(UUID org, UUID motion);

  long countByMotionIdAndChoice(UUID motion, VoteChoice choice);

  long countByMotionId(UUID motion);
}
