package org.civicops.volunteers.opportunity;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VolunteerOpportunityRepository
    extends JpaRepository<VolunteerOpportunity, UUID>,
        JpaSpecificationExecutor<VolunteerOpportunity> {
  Optional<VolunteerOpportunity> findByIdAndOrganizationId(UUID id, UUID orgId);

  Page<VolunteerOpportunity> findByOrganizationId(UUID orgId, Pageable pageable);

  Page<VolunteerOpportunity> findByOrganizationIdAndStatus(
      UUID orgId, OpportunityStatus status, Pageable pageable);

  long countByOrganizationIdAndStatus(UUID orgId, OpportunityStatus status);

  long countByOrganizationIdAndEventLinkedGrantId(UUID orgId, UUID grantId);
}
