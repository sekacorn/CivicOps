package org.civicops.equipment.checkout;

import jakarta.persistence.*;
import java.time.*;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.equipment.asset.*;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "equipment_checkout")
public class EquipmentCheckout extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "equipment_asset_id", nullable = false)
  private EquipmentAsset equipmentAsset;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "borrower_user_id")
  private User borrowerUser;

  @Column(length = 200)
  private String borrowerName;

  @Column(length = 320)
  private String borrowerEmail;

  @Column(nullable = false)
  private Instant checkedOutAt;

  @Column(nullable = false)
  private Instant dueAt;

  private Instant checkedInAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EquipmentCondition checkoutCondition;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private EquipmentCondition returnCondition;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CheckoutStatus status = CheckoutStatus.ACTIVE;

  @Column(columnDefinition = "TEXT")
  private String notes;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "issued_by_user_id", nullable = false)
  private User issuedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "received_by_user_id")
  private User receivedBy;

  protected EquipmentCheckout() {}

  public EquipmentCheckout(
      Organization o,
      EquipmentAsset a,
      User borrower,
      String name,
      String email,
      Instant out,
      Instant due,
      EquipmentCondition condition,
      String notes,
      User issuer) {
    organization = o;
    equipmentAsset = a;
    borrowerUser = borrower;
    borrowerName = name;
    borrowerEmail = email;
    checkedOutAt = out;
    dueAt = due;
    checkoutCondition = condition;
    this.notes = notes;
    issuedBy = issuer;
  }

  public Organization getOrganization() {
    return organization;
  }

  public EquipmentAsset getEquipmentAsset() {
    return equipmentAsset;
  }

  public User getBorrowerUser() {
    return borrowerUser;
  }

  public String getBorrowerName() {
    return borrowerName;
  }

  public String getBorrowerEmail() {
    return borrowerUser == null ? borrowerEmail : borrowerUser.getEmail();
  }

  public Instant getCheckedOutAt() {
    return checkedOutAt;
  }

  public Instant getDueAt() {
    return dueAt;
  }

  public Instant getCheckedInAt() {
    return checkedInAt;
  }

  public EquipmentCondition getCheckoutCondition() {
    return checkoutCondition;
  }

  public EquipmentCondition getReturnCondition() {
    return returnCondition;
  }

  public CheckoutStatus getStatus() {
    return status;
  }

  public String getNotes() {
    return notes;
  }

  public User getIssuedBy() {
    return issuedBy;
  }

  public User getReceivedBy() {
    return receivedBy;
  }

  public boolean isOverdue(Instant now) {
    return status == CheckoutStatus.ACTIVE && checkedInAt == null && dueAt.isBefore(now);
  }

  public String borrowerDisplayName() {
    return borrowerUser == null
        ? borrowerName
        : (borrowerUser.getFirstName() + " " + borrowerUser.getLastName()).trim();
  }

  public void checkIn(
      Instant now, EquipmentCondition condition, User receiver, String additionalNotes) {
    if (status != CheckoutStatus.ACTIVE)
      throw new BusinessRuleException(
          "CHECKOUT_NOT_ACTIVE", "Only active checkouts can be checked in");
    checkedInAt = now;
    returnCondition = condition;
    receivedBy = receiver;
    status = CheckoutStatus.RETURNED;
    if (additionalNotes != null) notes = additionalNotes;
  }

  public void markLost() {
    if (status != CheckoutStatus.ACTIVE)
      throw new BusinessRuleException(
          "CHECKOUT_NOT_ACTIVE", "Only active checkouts can be marked lost");
    status = CheckoutStatus.LOST;
  }
}
