package org.civicops.donations.campaign;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "donation_campaign")
public class DonationCampaign extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal goalAmount;

  private LocalDate startDate;
  private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CampaignStatus status = CampaignStatus.DRAFT;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by_user_id", nullable = false, updatable = false)
  private User createdBy;

  protected DonationCampaign() {}

  public DonationCampaign(
      Organization organization,
      User createdBy,
      String name,
      String description,
      BigDecimal goalAmount,
      LocalDate startDate,
      LocalDate endDate) {
    this.organization = organization;
    this.createdBy = createdBy;
    this.name = name;
    this.description = description;
    this.goalAmount = goalAmount;
    this.startDate = startDate;
    this.endDate = endDate;
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

  public BigDecimal getGoalAmount() {
    return goalAmount;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public CampaignStatus getStatus() {
    return status;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public void update(
      String name,
      String description,
      BigDecimal goalAmount,
      LocalDate startDate,
      LocalDate endDate) {
    if (status == CampaignStatus.CLOSED || status == CampaignStatus.CANCELLED)
      throw new BusinessRuleException(
          "TERMINAL_CAMPAIGN_IMMUTABLE", "Closed or cancelled campaigns cannot be edited");
    if (name != null) this.name = name;
    if (description != null) this.description = description;
    if (goalAmount != null) this.goalAmount = goalAmount;
    if (startDate != null) this.startDate = startDate;
    if (endDate != null) this.endDate = endDate;
  }

  public void transition(CampaignStatus target) {
    boolean allowed =
        switch (status) {
          case DRAFT -> target == CampaignStatus.ACTIVE || target == CampaignStatus.CANCELLED;
          case ACTIVE -> target == CampaignStatus.CLOSED || target == CampaignStatus.CANCELLED;
          case CLOSED, CANCELLED -> false;
        };
    if (!allowed)
      throw new BusinessRuleException(
          "INVALID_CAMPAIGN_TRANSITION",
          "Campaign cannot transition from " + status + " to " + target);
    status = target;
  }
}
