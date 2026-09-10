package org.civicops.equipment.category;

import jakarta.persistence.*;
import org.civicops.core.organization.Organization;
import org.civicops.shared.domain.BaseEntity;

@Entity
@Table(name = "equipment_category")
public class EquipmentCategory extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(name = "normalized_name", nullable = false, length = 120)
  private String normalizedName;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false)
  private boolean active = true;

  protected EquipmentCategory() {}

  public EquipmentCategory(Organization o, String name, String normalized, String description) {
    organization = o;
    this.name = name;
    normalizedName = normalized;
    this.description = description;
  }

  public Organization getOrganization() {
    return organization;
  }

  public String getName() {
    return name;
  }

  public String getNormalizedName() {
    return normalizedName;
  }

  public String getDescription() {
    return description;
  }

  public boolean isActive() {
    return active;
  }

  public void update(String name, String normalized, String description, Boolean active) {
    if (name != null) {
      this.name = name;
      normalizedName = normalized;
    }
    if (description != null) this.description = description;
    if (active != null) this.active = active;
  }
}
