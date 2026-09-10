package org.civicops.foodpantry.inventory;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PantryInventoryLotRepository
    extends JpaRepository<PantryInventoryLot, UUID>, JpaSpecificationExecutor<PantryInventoryLot> {
  Optional<PantryInventoryLot> findByIdAndOrganizationId(UUID id, UUID org);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select l from PantryInventoryLot l where l.organization.id=:org and l.pantryLocation.id=:pantry and l.pantryItem.id=:item and l.quantityRemaining>0 and (l.expirationDate is null or l.expirationDate>=:today) order by case when l.expirationDate is null then 1 else 0 end,l.expirationDate,l.receivedDate,l.id")
  List<PantryInventoryLot> findAvailableForUpdate(
      @Param("org") UUID org,
      @Param("pantry") UUID pantry,
      @Param("item") UUID item,
      @Param("today") LocalDate today);
}
