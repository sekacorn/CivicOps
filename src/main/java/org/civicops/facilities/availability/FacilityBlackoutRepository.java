package org.civicops.facilities.availability;

import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface FacilityBlackoutRepository
    extends JpaRepository<FacilityBlackout, UUID>, JpaSpecificationExecutor<FacilityBlackout> {
  Optional<FacilityBlackout> findByIdAndOrganizationId(UUID id, UUID org);

  @Query(
      "select count(b) from FacilityBlackout b where b.organization.id=:org and b.active=true and b.facility.id=:facility and (b.facilitySpace is null or b.facilitySpace.id=:space) and b.startDateTime<:end and b.endDateTime>:start")
  long conflicts(UUID org, UUID facility, UUID space, Instant start, Instant end);
}
