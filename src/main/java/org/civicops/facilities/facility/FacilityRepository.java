package org.civicops.facilities.facility;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface FacilityRepository
    extends JpaRepository<Facility, UUID>, JpaSpecificationExecutor<Facility> {
  Optional<Facility> findByIdAndOrganizationId(UUID id, UUID org);

  long countByOrganizationIdAndActiveTrue(UUID org);
}
