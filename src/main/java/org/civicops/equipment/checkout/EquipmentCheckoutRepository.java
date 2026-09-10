package org.civicops.equipment.checkout;

import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface EquipmentCheckoutRepository
    extends JpaRepository<EquipmentCheckout, UUID>, JpaSpecificationExecutor<EquipmentCheckout> {
  Optional<EquipmentCheckout> findByIdAndOrganizationId(UUID id, UUID org);

  long countByOrganizationIdAndStatusAndDueAtBefore(UUID org, CheckoutStatus status, Instant now);

  long countByEquipmentAssetIdAndStatus(UUID asset, CheckoutStatus status);
}
