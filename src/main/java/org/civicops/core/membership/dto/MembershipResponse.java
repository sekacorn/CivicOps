package org.civicops.core.membership.dto;

import java.time.Instant;
import java.util.UUID;
import org.civicops.core.membership.OrganizationMembership;
import org.civicops.core.membership.Role;

public record MembershipResponse(
    UUID id,
    UUID organizationId,
    UUID userId,
    Role role,
    boolean active,
    Instant joinedAt,
    Instant createdAt,
    Instant updatedAt) {
  public static MembershipResponse from(OrganizationMembership membership) {
    return new MembershipResponse(
        membership.getId(),
        membership.getOrganization().getId(),
        membership.getUser().getId(),
        membership.getRole(),
        membership.isActive(),
        membership.getJoinedAt(),
        membership.getCreatedAt(),
        membership.getUpdatedAt());
  }
}
