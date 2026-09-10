package org.civicops.foodpantry.distribution;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PantryDistributionVisitRepository
    extends JpaRepository<PantryDistributionVisit, UUID>,
        JpaSpecificationExecutor<PantryDistributionVisit> {
  Optional<PantryDistributionVisit> findByIdAndOrganizationId(UUID id, UUID org);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select v from PantryDistributionVisit v where v.id=:id and v.organization.id=:org")
  Optional<PantryDistributionVisit> findLocked(@Param("org") UUID org, @Param("id") UUID id);
}
