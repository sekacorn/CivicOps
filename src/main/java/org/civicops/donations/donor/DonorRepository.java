package org.civicops.donations.donor;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface DonorRepository
    extends JpaRepository<Donor, UUID>, JpaSpecificationExecutor<Donor> {
  Optional<Donor> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
