package org.civicops.cases.casefile;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface CaseRecordRepository
    extends JpaRepository<CaseRecord, UUID>, JpaSpecificationExecutor<CaseRecord> {
  Optional<CaseRecord> findByIdAndOrganizationId(UUID id, UUID org);

  boolean existsByOrganizationIdAndCaseNumber(UUID org, String number);

  boolean existsByOrganizationIdAndClientIdAndAssignedUserId(UUID org, UUID client, UUID user);

  long countByOrganizationIdAndStatus(UUID org, CaseStatus status);

  long countByOrganizationIdAndStatusAndClosedDateBetween(
      UUID org, CaseStatus status, java.time.LocalDate from, java.time.LocalDate to);
}
