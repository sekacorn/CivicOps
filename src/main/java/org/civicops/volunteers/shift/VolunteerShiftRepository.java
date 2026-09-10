package org.civicops.volunteers.shift;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface VolunteerShiftRepository
    extends JpaRepository<VolunteerShift, UUID>, JpaSpecificationExecutor<VolunteerShift> {
  Optional<VolunteerShift> findByIdAndOrganizationId(UUID id, UUID orgId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from VolunteerShift s where s.id=:id and s.organization.id=:orgId")
  Optional<VolunteerShift> findForRegistration(@Param("id") UUID id, @Param("orgId") UUID orgId);

  Page<VolunteerShift> findByOrganizationIdAndOpportunityId(
      UUID orgId, UUID opportunityId, Pageable pageable);

  long countByOrganizationIdAndStartAtAfter(UUID orgId, Instant now);

  @Query(
      "select count(s) from VolunteerShift s where s.opportunity.id=:opportunityId and"
          + " (s.startAt<:startAt or s.endAt>:endAt)")
  long countOutsideWindow(
      @Param("opportunityId") UUID opportunityId,
      @Param("startAt") Instant startAt,
      @Param("endAt") Instant endAt);

  @Query(
      value =
          "SELECT COALESCE(SUM(GREATEST(s.capacity - COALESCE(a.active_count, 0), 0)), 0) FROM"
              + " volunteer_shift s JOIN volunteer_opportunity o ON o.id=s.opportunity_id LEFT JOIN"
              + " (SELECT shift_id, COUNT(*) active_count FROM volunteer_assignment WHERE status <>"
              + " 'CANCELLED' GROUP BY shift_id) a ON a.shift_id=s.id WHERE"
              + " s.organization_id=:orgId AND o.status='OPEN' AND s.start_at>:now",
      nativeQuery = true)
  long openCapacity(@Param("orgId") UUID orgId, @Param("now") Instant now);
}
