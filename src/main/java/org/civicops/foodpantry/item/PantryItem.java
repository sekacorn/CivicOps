package org.civicops.foodpantry.item;

import jakarta.persistence.*;
import java.math.*;
import java.util.Locale;
import org.civicops.core.organization.Organization;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "pantry_item")
public class PantryItem extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  private String sku;
  private String normalizedSku;

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FoodCategory category;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UnitType unitType;

  @Column(nullable = false)
  private boolean active = true;

  @Column(nullable = false)
  private boolean trackExpiration;

  @Column(precision = 19, scale = 3)
  private BigDecimal reorderThreshold;

  @Column(columnDefinition = "TEXT")
  private String notes;

  protected PantryItem() {}

  public PantryItem(
      Organization o,
      String sku,
      String name,
      String description,
      FoodCategory category,
      UnitType unitType,
      boolean trackExpiration,
      BigDecimal threshold,
      String notes) {
    organization = o;
    update(sku, name, description, category, unitType, trackExpiration, threshold, notes);
  }

  public void update(
      String sku,
      String name,
      String description,
      FoodCategory category,
      UnitType unitType,
      Boolean trackExpiration,
      BigDecimal threshold,
      String notes) {
    if (sku != null) {
      this.sku = clean(sku);
      normalizedSku = normalizeSku(sku);
    }
    if (name != null) {
      if (name.isBlank())
        throw new BusinessRuleException("INVALID_PANTRY_ITEM", "Item name is required");
      this.name = name.trim();
    }
    if (description != null) this.description = clean(description);
    if (category != null) this.category = category;
    if (unitType != null) this.unitType = unitType;
    if (trackExpiration != null) this.trackExpiration = trackExpiration;
    if (threshold != null) {
      if (threshold.signum() < 0)
        throw new BusinessRuleException(
            "INVALID_REORDER_THRESHOLD", "Reorder threshold cannot be negative");
      reorderThreshold = quantity(threshold);
    }
    if (notes != null) this.notes = clean(notes);
    if (this.name == null || this.category == null || this.unitType == null)
      throw new BusinessRuleException(
          "INVALID_PANTRY_ITEM", "Name, category, and unit type are required");
  }

  public void deactivate() {
    active = false;
  }

  public void activate() {
    active = true;
  }

  public static String normalizeSku(String v) {
    String c = clean(v);
    return c == null ? null : c.toUpperCase(Locale.ROOT);
  }

  public static BigDecimal quantity(BigDecimal v) {
    return v.setScale(3, RoundingMode.HALF_UP);
  }

  private static String clean(String v) {
    return v == null || v.isBlank() ? null : v.trim();
  }

  public Organization getOrganization() {
    return organization;
  }

  public String getSku() {
    return sku;
  }

  public String getNormalizedSku() {
    return normalizedSku;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public FoodCategory getCategory() {
    return category;
  }

  public UnitType getUnitType() {
    return unitType;
  }

  public boolean isActive() {
    return active;
  }

  public boolean isTrackExpiration() {
    return trackExpiration;
  }

  public BigDecimal getReorderThreshold() {
    return reorderThreshold;
  }

  public String getNotes() {
    return notes;
  }
}
