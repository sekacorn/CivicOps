package org.civicops.scholarships.review;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface ScholarshipReviewAssignmentRepository
    extends JpaRepository<ScholarshipReviewAssignment, UUID>,
        JpaSpecificationExecutor<ScholarshipReviewAssignment> {
  Optional<ScholarshipReviewAssignment> findByIdAndOrganizationId(UUID id, UUID org);

  boolean existsByApplicationIdAndReviewerId(UUID application, UUID reviewer);

  boolean existsByApplicationIdAndReviewerIdAndStatusNot(
      UUID application, UUID reviewer, ReviewAssignmentStatus status);

  Page<ScholarshipReviewAssignment> findAllByOrganizationIdAndReviewerId(
      UUID org, UUID reviewer, Pageable pageable);

  long countByOrganizationId(UUID org);

  long countByOrganizationIdAndStatus(UUID org, ReviewAssignmentStatus status);
}
