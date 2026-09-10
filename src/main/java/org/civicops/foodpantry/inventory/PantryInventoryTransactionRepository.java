package org.civicops.foodpantry.inventory;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface PantryInventoryTransactionRepository
    extends JpaRepository<PantryInventoryTransaction, UUID>,
        JpaSpecificationExecutor<PantryInventoryTransaction> {
  List<PantryInventoryTransaction> findAllByOrganizationIdAndInventoryLotIdOrderByOccurredAtAsc(
      UUID org, UUID lot);
}
