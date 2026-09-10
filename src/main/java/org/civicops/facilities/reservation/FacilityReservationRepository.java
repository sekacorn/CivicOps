package org.civicops.facilities.reservation;

import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface FacilityReservationRepository
    extends JpaRepository<FacilityReservation, UUID>,
        JpaSpecificationExecutor<FacilityReservation> {
  Optional<FacilityReservation> findByIdAndOrganizationId(UUID id, UUID org);

  long countByOrganizationIdAndStatus(UUID org, ReservationStatus status);

  @Query(
      "select count(r) from FacilityReservation r where r.organization.id=:org and r.facilitySpace.id=:space and r.status='APPROVED' and r.id<>:exclude and r.startDateTime<:end and r.endDateTime>:start")
  long approvedConflicts(UUID org, UUID space, UUID exclude, Instant start, Instant end);

  @Query(
      "select count(r) from FacilityReservation r where r.organization.id=:org and r.startDateTime<:end and r.endDateTime>:start")
  long inPeriod(UUID org, Instant start, Instant end);
}
