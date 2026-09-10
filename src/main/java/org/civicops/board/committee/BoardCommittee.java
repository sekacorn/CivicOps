package org.civicops.board.committee;

import jakarta.persistence.*;
import org.civicops.core.organization.Organization;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "board_committee")
public class BoardCommittee extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false)
  private boolean active = true;

  protected BoardCommittee() {}

  public BoardCommittee(Organization o, String n, String d) {
    organization = o;
    update(n, d);
  }

  public void update(String n, String d) {
    if (n != null) {
      if (n.isBlank())
        throw new BusinessRuleException("INVALID_COMMITTEE", "Committee name is required");
      name = n.trim();
    }
    if (d != null) description = d.isBlank() ? null : d.trim();
  }

  public void deactivate() {
    active = false;
  }

  public Organization getOrganization() {
    return organization;
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
}
