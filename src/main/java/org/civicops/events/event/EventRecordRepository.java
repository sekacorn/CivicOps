package org.civicops.events.event;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EventRecordRepository
    extends JpaRepository<EventRecord, UUID>, JpaSpecificationExecutor<EventRecord> {
  Optional<EventRecord> findByIdAndOrganizationId(UUID id, UUID organizationId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from EventRecord e where e.id=:id and e.organization.id=:organizationId")
  Optional<EventRecord> findLockedByIdAndOrganizationId(
      @Param("id") UUID id, @Param("organizationId") UUID organizationId);

  long countByOrganizationIdAndStatus(UUID orgId, EventStatus status);

  List<EventRecord> findByOrganizationIdAndStartDateTimeBetween(
      UUID orgId, java.time.Instant from, java.time.Instant to);
}
