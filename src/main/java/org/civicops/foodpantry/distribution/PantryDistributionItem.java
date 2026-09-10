package org.civicops.foodpantry.distribution;

import jakarta.persistence.*;
import java.math.*;
import org.civicops.core.organization.Organization;
import org.civicops.foodpantry.item.PantryItem;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "pantry_distribution_item")
public class PantryDistributionItem extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "distribution_visit_id")
  private PantryDistributionVisit distributionVisit;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pantry_item_id")
  private PantryItem pantryItem;

  @Column(precision = 19, scale = 3)
  private BigDecimal quantity;

  protected PantryDistributionItem() {}

  public PantryDistributionItem(PantryDistributionVisit v, PantryItem i, BigDecimal q) {
    organization = v.getOrganization();
    distributionVisit = v;
    pantryItem = i;
    quantity = q.setScale(3, RoundingMode.HALF_UP);
    if (quantity.signum() <= 0)
      throw new BusinessRuleException(
          "INVALID_DISTRIBUTION_QUANTITY", "Distribution quantity must be positive");
  }

  public Organization getOrganization() {
    return organization;
  }

  public PantryDistributionVisit getDistributionVisit() {
    return distributionVisit;
  }

  public PantryItem getPantryItem() {
    return pantryItem;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }
}
