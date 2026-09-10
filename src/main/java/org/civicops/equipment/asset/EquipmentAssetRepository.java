package org.civicops.equipment.asset;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EquipmentAssetRepository
    extends JpaRepository<EquipmentAsset, UUID>, JpaSpecificationExecutor<EquipmentAsset> {
  Optional<EquipmentAsset> findByIdAndOrganizationId(UUID id, UUID org);

  boolean existsByOrganizationIdAndAssetTag(UUID org, String tag);

  long countByOrganizationId(UUID org);

  long countByOrganizationIdAndStatus(UUID org, AssetStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from EquipmentAsset a where a.id=:id and a.organization.id=:org")
  Optional<EquipmentAsset> findLocked(@Param("id") UUID id, @Param("org") UUID org);
}
