package org.civicops.core.security;

import org.civicops.core.membership.OrganizationMembership;
import org.civicops.core.membership.OrganizationMembershipRepository;
import org.civicops.core.membership.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Central authorization seam for organization-owned resources once authenticated principals are introduced. */
@Service
public class OrganizationAccessService {
    private final OrganizationMembershipRepository memberships;

    public OrganizationAccessService(OrganizationMembershipRepository memberships) {
        this.memberships = memberships;
    }

    @Transactional(readOnly = true)
    public OrganizationMembership requireAnyRole(UUID userId, UUID organizationId, Set<Role> allowedRoles) {
        OrganizationMembership membership = requireMembership(userId, organizationId);
        if (!membership.getRole().grantsAny(allowedRoles)) {
            throw new AccessDeniedException("The membership role does not permit this action");
        }
        return membership;
    }

    @Transactional(readOnly = true)
    public OrganizationMembership requireMembership(UUID userId, UUID organizationId) {
        return memberships.findByOrganizationIdAndUserIdAndActiveTrue(organizationId, userId)
                .orElseThrow(() -> new AccessDeniedException("No active membership for this organization"));
    }

    @Transactional(readOnly = true)
    public OrganizationMembership requireRole(UUID userId, UUID organizationId, Role role) {
        return requireAnyRole(userId, organizationId, Set.of(role));
    }

    @Transactional(readOnly = true)
    public OrganizationMembership requireAnyRole(UUID userId, UUID organizationId, Role... roles) {
        return requireAnyRole(userId, organizationId, Set.copyOf(Arrays.asList(roles)));
    }

    @Transactional(readOnly = true)
    public boolean hasMembership(UUID userId, UUID organizationId) {
        return memberships.findByOrganizationIdAndUserIdAndActiveTrue(organizationId, userId).isPresent();
    }

    @Transactional(readOnly = true)
    public List<org.civicops.core.organization.Organization> organizationsForUser(UUID userId) {
        return memberships.findActiveOrganizationsForUser(userId).stream()
                .map(OrganizationMembership::getOrganization).toList();
    }
}
