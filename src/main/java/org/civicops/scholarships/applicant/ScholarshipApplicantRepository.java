package org.civicops.scholarships.applicant;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ScholarshipApplicantRepository
    extends JpaRepository<ScholarshipApplicant, UUID>,
        JpaSpecificationExecutor<ScholarshipApplicant> {
  Optional<ScholarshipApplicant> findByIdAndOrganizationId(UUID id, UUID org);

  boolean existsByOrganizationIdAndNormalizedEmail(UUID org, String email);
}
