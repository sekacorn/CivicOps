package org.civicops.cases.client;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ClientRepository
    extends JpaRepository<Client, UUID>, JpaSpecificationExecutor<Client> {
  Optional<Client> findByIdAndOrganizationId(UUID id, UUID org);

  boolean existsByOrganizationIdAndExternalReferenceNumber(UUID org, String ref);

  long countByOrganizationIdAndActiveTrue(UUID org);
}
