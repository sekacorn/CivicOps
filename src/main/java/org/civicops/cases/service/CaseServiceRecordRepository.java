package org.civicops.cases.service;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface CaseServiceRecordRepository
    extends JpaRepository<CaseServiceRecord, UUID>, JpaSpecificationExecutor<CaseServiceRecord> {
  Optional<CaseServiceRecord> findByIdAndOrganizationIdAndCaseRecordId(
      UUID id, UUID org, UUID caseId);
}
