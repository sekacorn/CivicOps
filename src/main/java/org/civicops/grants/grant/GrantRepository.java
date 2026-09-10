package org.civicops.grants.grant;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface GrantRepository
    extends JpaRepository<Grant, UUID>, JpaSpecificationExecutor<Grant> {
  Optional<Grant> findByIdAndOrganizationId(UUID id, UUID organizationId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select g from Grant g where g.id = :id and g.organization.id = :organizationId")
  Optional<Grant> findLockedByIdAndOrganizationId(
      @Param("id") UUID id, @Param("organizationId") UUID organizationId);

  List<Grant> findAllByOrganizationId(UUID organizationId);
}
