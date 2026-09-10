package org.civicops.scholarships.application;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ScholarshipApplicationRepository
    extends JpaRepository<ScholarshipApplication, UUID>,
        JpaSpecificationExecutor<ScholarshipApplication> {
  Optional<ScholarshipApplication> findByIdAndOrganizationId(UUID id, UUID org);

  boolean existsByProgramIdAndApplicantId(UUID program, UUID applicant);

  long countByProgramIdAndStatus(UUID program, ScholarshipApplicationStatus status);

  long countByOrganizationIdAndStatus(UUID org, ScholarshipApplicationStatus status);
}
