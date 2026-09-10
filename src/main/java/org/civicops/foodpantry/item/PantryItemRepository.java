package org.civicops.foodpantry.item;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface PantryItemRepository
    extends JpaRepository<PantryItem, UUID>, JpaSpecificationExecutor<PantryItem> {
  Optional<PantryItem> findByIdAndOrganizationId(UUID id, UUID org);

  boolean existsByOrganizationIdAndNormalizedSku(UUID org, String sku);
}
