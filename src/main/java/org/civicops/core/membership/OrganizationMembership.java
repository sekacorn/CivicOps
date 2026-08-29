package org.civicops.core.membership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.shared.domain.BaseEntity;
import java.time.Instant;

@Entity
@Table(name = "organization_membership", uniqueConstraints =
        @UniqueConstraint(name = "uk_membership_organization_user", columnNames = {"organization_id", "user_id"}))
public class OrganizationMembership extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    protected OrganizationMembership() {
    }

    OrganizationMembership(Organization organization, User user, Role role, Instant joinedAt) {
        this.organization = organization;
        this.user = user;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public Organization getOrganization() { return organization; }
    public User getUser() { return user; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
    public Instant getJoinedAt() { return joinedAt; }
}
