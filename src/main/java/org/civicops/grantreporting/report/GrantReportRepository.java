package org.civicops.grantreporting.report;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface GrantReportRepository
    extends JpaRepository<GrantReport, UUID>, JpaSpecificationExecutor<GrantReport> {
  Optional<GrantReport> findByIdAndOrganizationId(UUID id, UUID org);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from GrantReport r where r.id=:id and r.organization.id=:org")
  Optional<GrantReport> findLocked(@Param("org") UUID org, @Param("id") UUID id);
}
