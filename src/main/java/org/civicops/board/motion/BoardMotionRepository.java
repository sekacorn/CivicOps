package org.civicops.board.motion;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface BoardMotionRepository
    extends JpaRepository<BoardMotion, UUID>, JpaSpecificationExecutor<BoardMotion> {
  Optional<BoardMotion> findByIdAndOrganizationId(UUID id, UUID org);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select m from BoardMotion m where m.id=:id and m.organization.id=:org")
  Optional<BoardMotion> findLocked(@Param("org") UUID org, @Param("id") UUID id);

  long countByOrganizationIdAndStatus(UUID org, MotionStatus status);
}
