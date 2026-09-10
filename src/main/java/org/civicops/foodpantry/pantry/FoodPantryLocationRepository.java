package org.civicops.foodpantry.pantry;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface FoodPantryLocationRepository
    extends JpaRepository<FoodPantryLocation, UUID>, JpaSpecificationExecutor<FoodPantryLocation> {
  Optional<FoodPantryLocation> findByIdAndOrganizationId(UUID id, UUID org);
}
