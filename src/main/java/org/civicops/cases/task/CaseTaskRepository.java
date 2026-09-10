package org.civicops.cases.task;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface CaseTaskRepository
    extends JpaRepository<CaseTask, UUID>, JpaSpecificationExecutor<CaseTask> {
  Optional<CaseTask> findByIdAndOrganizationIdAndCaseRecordId(UUID id, UUID org, UUID caseId);

  long countByOrganizationIdAndDueDateBeforeAndStatusNotIn(
      UUID org, java.time.LocalDate date, Collection<CaseTaskStatus> statuses);
}
