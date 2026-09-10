package org.civicops.foodpantry.distribution;

import jakarta.persistence.*;
import java.time.Instant;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.foodpantry.household.PantryHousehold;
import org.civicops.foodpantry.pantry.FoodPantryLocation;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "pantry_distribution_visit")
public class PantryDistributionVisit extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pantry_location_id")
  private FoodPantryLocation pantryLocation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "household_id")
  private PantryHousehold household;

  private String recipientName;
  private Instant visitDateTime;
  private int householdSizeAtVisit;

  @Column(columnDefinition = "TEXT")
  private String notes;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "served_by_user_id")
  private User servedBy;

  @Enumerated(EnumType.STRING)
  private DistributionVisitStatus status = DistributionVisitStatus.OPEN;

  private Instant completedAt;
  private Instant cancelledAt;

  protected PantryDistributionVisit() {}

  public PantryDistributionVisit(
      FoodPantryLocation p,
      PantryHousehold h,
      String recipient,
      Instant visit,
      int size,
      String notes,
      User servedBy) {
    organization = p.getOrganization();
    pantryLocation = p;
    household = h;
    recipientName = clean(recipient);
    visitDateTime = visit;
    householdSizeAtVisit = size;
    this.notes = clean(notes);
    this.servedBy = servedBy;
    if (h == null && recipientName == null)
      throw new BusinessRuleException(
          "RECIPIENT_REQUIRED", "Household or recipient name is required");
    if (size <= 0 || visit == null)
      throw new BusinessRuleException(
          "INVALID_DISTRIBUTION_VISIT", "Visit time and positive household size are required");
  }

  public void complete(Instant now) {
    if (status != DistributionVisitStatus.OPEN) invalid();
    status = DistributionVisitStatus.COMPLETED;
    completedAt = now;
  }

  public void cancel(Instant now) {
    if (status != DistributionVisitStatus.OPEN) invalid();
    status = DistributionVisitStatus.CANCELLED;
    cancelledAt = now;
  }

  private void invalid() {
    throw new BusinessRuleException(
        "INVALID_DISTRIBUTION_TRANSITION", "Distribution visit is not open");
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

  public PantryHousehold getHousehold() {
    return household;
  }

  public String getRecipientName() {
    return recipientName;
  }

  public Instant getVisitDateTime() {
    return visitDateTime;
  }

  public int getHouseholdSizeAtVisit() {
    return householdSizeAtVisit;
  }

  public String getNotes() {
    return notes;
  }

  public User getServedBy() {
    return servedBy;
  }

  public DistributionVisitStatus getStatus() {
    return status;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public Instant getCancelledAt() {
    return cancelledAt;
  }
}
