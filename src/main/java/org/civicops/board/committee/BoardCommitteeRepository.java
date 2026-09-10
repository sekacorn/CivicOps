package org.civicops.board.committee;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface BoardCommitteeRepository
    extends JpaRepository<BoardCommittee, UUID>, JpaSpecificationExecutor<BoardCommittee> {
  Optional<BoardCommittee> findByIdAndOrganizationId(UUID id, UUID org);

  boolean existsByOrganizationIdAndNameIgnoreCase(UUID org, String name);

  long countByOrganizationIdAndActiveTrue(UUID org);
}
