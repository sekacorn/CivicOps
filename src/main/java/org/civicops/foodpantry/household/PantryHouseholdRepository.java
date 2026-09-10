package org.civicops.foodpantry.household;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface PantryHouseholdRepository
    extends JpaRepository<PantryHousehold, UUID>, JpaSpecificationExecutor<PantryHousehold> {
  Optional<PantryHousehold> findByIdAndOrganizationId(UUID id, UUID org);

  boolean existsByOrganizationIdAndExternalReferenceNumber(UUID org, String ref);

  long countByOrganizationIdAndActiveTrue(UUID org);
}
