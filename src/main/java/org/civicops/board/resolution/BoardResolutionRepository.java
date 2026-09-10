package org.civicops.board.resolution;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface BoardResolutionRepository
    extends JpaRepository<BoardResolution, UUID>, JpaSpecificationExecutor<BoardResolution> {
  Optional<BoardResolution> findByIdAndOrganizationId(UUID id, UUID org);

  boolean existsByOrganizationIdAndResolutionNumberIgnoreCase(UUID org, String number);

  long countByOrganizationIdAndStatus(UUID org, ResolutionStatus status);
}
