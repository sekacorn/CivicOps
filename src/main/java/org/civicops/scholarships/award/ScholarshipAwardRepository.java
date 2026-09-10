package org.civicops.scholarships.award;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ScholarshipAwardRepository
    extends JpaRepository<ScholarshipAward, UUID>, JpaSpecificationExecutor<ScholarshipAward> {
  Optional<ScholarshipAward> findByIdAndOrganizationId(UUID id, UUID org);

  boolean existsByApplicationId(UUID application);

  long countByOrganizationId(UUID org);

  long countByProgramId(UUID program);

  @Query(
      "select coalesce(sum(a.amount),0) from ScholarshipAward a where a.organization.id=:org and a.status<>'CANCELLED'")
  BigDecimal totalByOrganization(UUID org);

  @Query(
      "select coalesce(sum(a.amount),0) from ScholarshipAward a where a.program.id=:program and a.status<>'CANCELLED'")
  BigDecimal totalByProgram(UUID program);
}
