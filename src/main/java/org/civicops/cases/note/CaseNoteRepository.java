package org.civicops.cases.note;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseNoteRepository extends JpaRepository<CaseNote, UUID> {
  Page<CaseNote> findAllByOrganizationIdAndCaseRecordId(UUID org, UUID caseId, Pageable p);

  Optional<CaseNote> findByIdAndOrganizationIdAndCaseRecordId(UUID id, UUID org, UUID caseId);
}
