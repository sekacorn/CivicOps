package org.civicops.scholarships.review;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ScholarshipReviewRepository extends JpaRepository<ScholarshipReview, UUID> {
  boolean existsByAssignmentId(UUID assignment);

  List<ScholarshipReview> findAllByAssignmentApplicationId(UUID application);

  @Query("select avg(r.score) from ScholarshipReview r where r.organization.id=:org")
  BigDecimal averageScore(UUID org);
}
