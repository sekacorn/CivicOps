package org.civicops.foodpantry.inventory;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.foodpantry.item.PantryItem;
import org.civicops.foodpantry.pantry.FoodPantryLocation;
import org.civicops.shared.domain.BaseEntity;

@Entity
@Table(name = "pantry_inventory_transaction")
public class PantryInventoryTransaction extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pantry_location_id")
  private FoodPantryLocation pantryLocation;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pantry_item_id")
  private PantryItem pantryItem;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "inventory_lot_id")
  private PantryInventoryLot inventoryLot;

  @Enumerated(EnumType.STRING)
  private InventoryTransactionType transactionType;

  private BigDecimal quantity;
  private Instant occurredAt;
  private String referenceType;
  private UUID referenceId;

  @Column(columnDefinition = "TEXT")
  private String notes;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by_user_id")
  private User createdBy;

  protected PantryInventoryTransaction() {}

  public PantryInventoryTransaction(
      PantryInventoryLot lot,
      InventoryTransactionType type,
      BigDecimal quantity,
      Instant occurred,
      String refType,
      UUID refId,
      String notes,
      User actor) {
    organization = lot.getOrganization();
    pantryLocation = lot.getPantryLocation();
    pantryItem = lot.getPantryItem();
    inventoryLot = lot;
    transactionType = type;
    this.quantity = quantity.setScale(3, java.math.RoundingMode.HALF_UP);
    occurredAt = occurred;
    referenceType = refType;
    referenceId = refId;
    this.notes = notes;
    createdBy = actor;
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

  public PantryInventoryLot getInventoryLot() {
    return inventoryLot;
  }

  public InventoryTransactionType getTransactionType() {
    return transactionType;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public String getReferenceType() {
    return referenceType;
  }

  public UUID getReferenceId() {
    return referenceId;
  }

  public String getNotes() {
    return notes;
  }

  public User getCreatedBy() {
    return createdBy;
  }
}
