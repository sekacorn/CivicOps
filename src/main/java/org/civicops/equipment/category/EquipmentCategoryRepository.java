package org.civicops.equipment.category;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentCategoryRepository extends JpaRepository<EquipmentCategory, UUID> {
  Optional<EquipmentCategory> findByIdAndOrganizationId(UUID id, UUID org);

  boolean existsByOrganizationIdAndNormalizedName(UUID org, String name);

  Page<EquipmentCategory> findAllByOrganizationId(UUID org, Pageable p);
}
