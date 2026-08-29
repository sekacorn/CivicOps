package org.civicops.core.membership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, UUID> {
    boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);
    Optional<OrganizationMembership> findByOrganizationIdAndUserIdAndActiveTrue(UUID organizationId, UUID userId);
    List<OrganizationMembership> findAllByOrganizationId(UUID organizationId);

    @Query("select membership from OrganizationMembership membership join fetch membership.organization " +
            "where membership.user.id = :userId and membership.active = true and membership.organization.active = true")
    List<OrganizationMembership> findActiveOrganizationsForUser(@Param("userId") UUID userId);
}
