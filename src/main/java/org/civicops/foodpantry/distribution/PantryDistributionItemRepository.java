package org.civicops.foodpantry.distribution;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface PantryDistributionItemRepository
    extends JpaRepository<PantryDistributionItem, UUID> {
  List<PantryDistributionItem> findAllByDistributionVisitIdOrderByPantryItemId(UUID visit);

  boolean existsByDistributionVisitIdAndPantryItemId(UUID visit, UUID item);
}
