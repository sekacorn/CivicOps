package org.civicops.board.meeting;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface BoardMeetingRepository
    extends JpaRepository<BoardMeeting, UUID>, JpaSpecificationExecutor<BoardMeeting> {
  Optional<BoardMeeting> findByIdAndOrganizationId(UUID id, UUID org);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select m from BoardMeeting m where m.id=:id and m.organization.id=:org")
  Optional<BoardMeeting> findLocked(@Param("org") UUID org, @Param("id") UUID id);

  @Query(
      "select count(m) from BoardMeeting m where m.organization.id=:org and m.startDateTime>=:from and m.startDateTime<:to")
  long inPeriod(@Param("org") UUID org, @Param("from") Instant from, @Param("to") Instant to);
}
