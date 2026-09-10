package org.civicops.facilities.space;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface FacilitySpaceRepository
    extends JpaRepository<FacilitySpace, UUID>, JpaSpecificationExecutor<FacilitySpace> {
  Optional<FacilitySpace> findByIdAndOrganizationId(UUID id, UUID org);

  boolean existsByFacilityIdAndNormalizedName(UUID facility, String name);

  long countByOrganizationIdAndActiveTrueAndReservableTrue(UUID org);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from FacilitySpace s where s.id=:id and s.organization.id=:org")
  Optional<FacilitySpace> findLocked(@Param("id") UUID id, @Param("org") UUID org);
}
