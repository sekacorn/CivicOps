package org.civicops.grantreporting.template;

import jakarta.persistence.*;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.grants.grant.Grant;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(
    name = "grant_report_template",
    uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "id"}))
public class GrantReportTemplate extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "grant_id")
  private Grant grant;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false)
  private boolean active = true;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by_user_id", updatable = false)
  private User createdBy;

  protected GrantReportTemplate() {}

  public GrantReportTemplate(Organization o, Grant g, String n, String d, User u) {
    organization = o;
    grant = g;
    name = n;
    description = d;
    createdBy = u;
  }

  public Organization getOrganization() {
    return organization;
  }

  public Grant getGrant() {
    return grant;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public boolean isActive() {
    return active;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public void update(String n, String d, Boolean a) {
    if (n != null) name = n;
    if (d != null) description = d;
    if (a != null) active = a;
  }

  public void requireUsableFor(Grant g) {
    if (!active)
      throw new BusinessRuleException(
          "INACTIVE_REPORT_TEMPLATE", "Inactive templates cannot create reports");
    if (grant != null && !grant.getId().equals(g.getId()))
      throw new BusinessRuleException(
          "GRANT_TEMPLATE_MISMATCH", "Grant-specific template belongs to another grant");
  }
}
