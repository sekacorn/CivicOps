package org.civicops.equipment.maintenance;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface EquipmentMaintenanceRepository
    extends JpaRepository<EquipmentMaintenanceRecord, UUID>,
        JpaSpecificationExecutor<EquipmentMaintenanceRecord> {
  Optional<EquipmentMaintenanceRecord> findByIdAndOrganizationId(UUID id, UUID org);

  long countByOrganizationIdAndStatusIn(UUID org, Collection<MaintenanceStatus> statuses);
}
