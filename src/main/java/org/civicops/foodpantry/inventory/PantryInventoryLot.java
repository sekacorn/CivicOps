package org.civicops.foodpantry.inventory;

import jakarta.persistence.*;
import java.math.*;
import java.time.LocalDate;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.foodpantry.item.PantryItem;
import org.civicops.foodpantry.pantry.FoodPantryLocation;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "pantry_inventory_lot")
public class PantryInventoryLot extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pantry_location_id")
  private FoodPantryLocation pantryLocation;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pantry_item_id")
  private PantryItem pantryItem;

  private String lotNumber;
  private LocalDate receivedDate;
  private LocalDate expirationDate;

  @Column(precision = 19, scale = 3)
  private BigDecimal quantityReceived;

  @Column(precision = 19, scale = 3)
  private BigDecimal quantityRemaining;

  @Enumerated(EnumType.STRING)
  private InventorySourceType sourceType;

  private String sourceReference;

  @Column(columnDefinition = "TEXT")
  private String notes;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by_user_id")
  private User createdBy;

  protected PantryInventoryLot() {}

  public PantryInventoryLot(
      FoodPantryLocation p,
      PantryItem i,
      String lot,
      LocalDate received,
      LocalDate expiration,
      BigDecimal quantity,
      InventorySourceType source,
      String reference,
      String notes,
      User actor) {
    organization = p.getOrganization();
    pantryLocation = p;
    pantryItem = i;
    lotNumber = clean(lot);
    receivedDate = received;
    expirationDate = expiration;
    quantityReceived = q(quantity);
    quantityRemaining = quantityReceived;
    sourceType = source;
    sourceReference = clean(reference);
    this.notes = clean(notes);
    createdBy = actor;
    validate();
  }

  private void validate() {
    if (receivedDate == null || quantityReceived == null || quantityReceived.signum() <= 0)
      throw new BusinessRuleException(
          "INVALID_INVENTORY_RECEIPT", "Received date and positive quantity are required");
    if (expirationDate != null && expirationDate.isBefore(receivedDate))
      throw new BusinessRuleException(
          "INVALID_EXPIRATION_DATE", "Expiration cannot precede received date");
    if (pantryItem.isTrackExpiration() && expirationDate == null)
      throw new BusinessRuleException(
          "EXPIRATION_REQUIRED", "Expiration date is required for this item");
  }

  public void consume(BigDecimal amount) {
    amount = q(amount);
    if (amount.signum() <= 0 || quantityRemaining.compareTo(amount) < 0)
      throw new BusinessRuleException(
          "INSUFFICIENT_INVENTORY", "Inventory lot does not contain enough stock");
    quantityRemaining = quantityRemaining.subtract(amount);
  }

  public void decrease(BigDecimal amount) {
    consume(amount);
  }

  public void increase(BigDecimal amount) {
    amount = q(amount);
    if (amount.signum() <= 0 || quantityRemaining.add(amount).compareTo(quantityReceived) > 0)
      throw new BusinessRuleException(
          "INVALID_INVENTORY_ADJUSTMENT",
          "Increase cannot exceed the originally received quantity");
    quantityRemaining = quantityRemaining.add(amount);
  }

  public boolean isExpired(LocalDate today) {
    return expirationDate != null && expirationDate.isBefore(today);
  }

  private static BigDecimal q(BigDecimal v) {
    return v == null ? null : v.setScale(3, RoundingMode.HALF_UP);
  }

  private static String clean(String v) {
    return v == null || v.isBlank() ? null : v.trim();
  }

  public Organization getOrganization() {
    return organization;
  }

  public FoodPantryLocation getPantryLocation() {
    return pantryLocation;
  }

  public PantryItem getPantryItem() {
    return pantryItem;
  }

  public String getLotNumber() {
    return lotNumber;
  }

  public LocalDate getReceivedDate() {
    return receivedDate;
  }

  public LocalDate getExpirationDate() {
    return expirationDate;
  }

  public BigDecimal getQuantityReceived() {
    return quantityReceived;
  }

  public BigDecimal getQuantityRemaining() {
    return quantityRemaining;
  }

  public InventorySourceType getSourceType() {
    return sourceType;
  }

  public String getSourceReference() {
    return sourceReference;
  }

  public String getNotes() {
    return notes;
  }

  public User getCreatedBy() {
    return createdBy;
  }
}
