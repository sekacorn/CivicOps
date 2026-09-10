package org.civicops.volunteers.volunteer;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VolunteerRepository
    extends JpaRepository<Volunteer, UUID>, JpaSpecificationExecutor<Volunteer> {
  Optional<Volunteer> findByIdAndOrganizationId(UUID id, UUID organizationId);

  Optional<Volunteer> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

  boolean existsByOrganizationIdAndEmail(UUID organizationId, String email);

  boolean existsByOrganizationIdAndEmailAndIdNot(UUID organizationId, String email, UUID id);

  boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);

  Page<Volunteer> findByOrganizationId(UUID organizationId, Pageable pageable);

  Page<Volunteer> findByOrganizationIdAndStatus(
      UUID organizationId, VolunteerStatus status, Pageable pageable);

  long countByOrganizationIdAndStatus(UUID organizationId, VolunteerStatus status);
}
