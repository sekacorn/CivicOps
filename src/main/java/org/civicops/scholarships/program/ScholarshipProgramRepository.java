package org.civicops.scholarships.program;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ScholarshipProgramRepository
    extends JpaRepository<ScholarshipProgram, UUID>, JpaSpecificationExecutor<ScholarshipProgram> {
  Optional<ScholarshipProgram> findByIdAndOrganizationId(UUID id, UUID org);

  long countByOrganizationIdAndStatusNotIn(UUID org, Collection<ScholarshipProgramStatus> statuses);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from ScholarshipProgram p where p.id=:id and p.organization.id=:org")
  Optional<ScholarshipProgram> findLocked(@Param("id") UUID id, @Param("org") UUID org);
}
