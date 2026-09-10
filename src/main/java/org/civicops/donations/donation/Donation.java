package org.civicops.donations.donation;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.donations.campaign.DonationCampaign;
import org.civicops.donations.donor.Donor;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "donation")
public class Donation extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "donor_id")
  private Donor donor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "campaign_id")
  private DonationCampaign campaign;

  @Column(nullable = false)
  private boolean anonymous;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false)
  private LocalDate donationDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private DonationPaymentMethod paymentMethod;

  @Column(length = 500)
  private String inKindDescription;

  @Column(nullable = false)
  private boolean restricted;

  @Column(columnDefinition = "TEXT")
  private String restrictionDescription;

  @Column(length = 200)
  private String designation;

  @Column(length = 100)
  private String referenceNumber;

  @Column(length = 100)
  private String receiptNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AcknowledgementStatus acknowledgementStatus;

  @Column(columnDefinition = "TEXT")
  private String notes;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private DonationStatus status = DonationStatus.RECORDED;

  private Instant reversedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reversed_by_user_id")
  private User reversedBy;

  @Column(length = 500)
  private String reversalReason;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by_user_id", nullable = false, updatable = false)
  private User createdBy;

  protected Donation() {}

  public Donation(
      Organization organization,
      Donor donor,
      DonationCampaign campaign,
      boolean anonymous,
      BigDecimal amount,
      LocalDate donationDate,
      DonationPaymentMethod paymentMethod,
      String inKindDescription,
      boolean restricted,
      String restrictionDescription,
      String designation,
      String referenceNumber,
      String receiptNumber,
      AcknowledgementStatus acknowledgementStatus,
      String notes,
      User createdBy) {
    this.organization = organization;
    this.donor = donor;
    this.campaign = campaign;
    this.anonymous = anonymous;
    this.amount = amount;
    this.donationDate = donationDate;
    this.paymentMethod = paymentMethod;
    this.inKindDescription = inKindDescription;
    this.restricted = restricted;
    this.restrictionDescription = restrictionDescription;
    this.designation = designation;
    this.referenceNumber = referenceNumber;
    this.receiptNumber = receiptNumber;
    this.acknowledgementStatus = acknowledgementStatus;
    this.notes = notes;
    this.createdBy = createdBy;
  }

  public Organization getOrganization() {
    return organization;
  }

  public Donor getDonor() {
    return donor;
  }

  public DonationCampaign getCampaign() {
    return campaign;
  }

  public boolean isAnonymous() {
    return anonymous;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public LocalDate getDonationDate() {
    return donationDate;
  }

  public DonationPaymentMethod getPaymentMethod() {
    return paymentMethod;
  }

  public String getInKindDescription() {
    return inKindDescription;
  }

  public boolean isRestricted() {
    return restricted;
  }

  public String getRestrictionDescription() {
    return restrictionDescription;
  }

  public String getDesignation() {
    return designation;
  }

  public String getReferenceNumber() {
    return referenceNumber;
  }

  public String getReceiptNumber() {
    return receiptNumber;
  }

  public AcknowledgementStatus getAcknowledgementStatus() {
    return acknowledgementStatus;
  }

  public String getNotes() {
    return notes;
  }

  public DonationStatus getStatus() {
    return status;
  }

  public Instant getReversedAt() {
    return reversedAt;
  }

  public User getReversedBy() {
    return reversedBy;
  }

  public String getReversalReason() {
    return reversalReason;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public void reverse(User user, String reason) {
    if (status != DonationStatus.RECORDED)
      throw new BusinessRuleException(
          "DONATION_ALREADY_REVERSED", "Donation has already been reversed");
    status = DonationStatus.REVERSED;
    reversedBy = user;
    reversedAt = Instant.now();
    reversalReason = reason;
  }
}
