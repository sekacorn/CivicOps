package org.civicops.grants.grant;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "grant_record")
public class Grant extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Column(nullable = false, length = 200)
  private String grantName;

  @Column(nullable = false, length = 200)
  private String grantorName;

  @Column(length = 100)
  private String grantNumber;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal awardAmount;

  private LocalDate applicationDeadline;
  private LocalDate submittedDate;
  private LocalDate awardDate;
  private LocalDate startDate;
  private LocalDate endDate;
  private LocalDate reportingDeadline;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private GrantStatus status = GrantStatus.PROSPECT;

  @Column(nullable = false)
  private boolean restricted;

  @Column(columnDefinition = "TEXT")
  private String restrictionDescription;

  @Column(length = 200)
  private String primaryContactName;

  @Column(length = 320)
  private String primaryContactEmail;

  @Column(columnDefinition = "TEXT")
  private String notes;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by_user_id", nullable = false, updatable = false)
  private User createdBy;

  protected Grant() {}

  public Grant(
      Organization organization,
      User createdBy,
      String grantName,
      String grantorName,
      String grantNumber,
      String description,
      BigDecimal awardAmount,
      LocalDate applicationDeadline,
      LocalDate submittedDate,
      LocalDate awardDate,
      LocalDate startDate,
      LocalDate endDate,
      LocalDate reportingDeadline,
      boolean restricted,
      String restrictionDescription,
      String primaryContactName,
      String primaryContactEmail,
      String notes) {
    this.organization = organization;
    this.createdBy = createdBy;
    this.grantName = grantName;
    this.grantorName = grantorName;
    this.grantNumber = grantNumber;
    this.description = description;
    this.awardAmount = awardAmount;
    this.applicationDeadline = applicationDeadline;
    this.submittedDate = submittedDate;
    this.awardDate = awardDate;
    this.startDate = startDate;
    this.endDate = endDate;
    this.reportingDeadline = reportingDeadline;
    this.restricted = restricted;
    this.restrictionDescription = restrictionDescription;
    this.primaryContactName = primaryContactName;
    this.primaryContactEmail = primaryContactEmail;
    this.notes = notes;
  }

  public Organization getOrganization() {
    return organization;
  }

  public String getGrantName() {
    return grantName;
  }

  public String getGrantorName() {
    return grantorName;
  }

  public String getGrantNumber() {
    return grantNumber;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getAwardAmount() {
    return awardAmount;
  }

  public LocalDate getApplicationDeadline() {
    return applicationDeadline;
  }

  public LocalDate getSubmittedDate() {
    return submittedDate;
  }

  public LocalDate getAwardDate() {
    return awardDate;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public LocalDate getReportingDeadline() {
    return reportingDeadline;
  }

  public GrantStatus getStatus() {
    return status;
  }

  public boolean isRestricted() {
    return restricted;
  }

  public String getRestrictionDescription() {
    return restrictionDescription;
  }

  public String getPrimaryContactName() {
    return primaryContactName;
  }

  public String getPrimaryContactEmail() {
    return primaryContactEmail;
  }

  public String getNotes() {
    return notes;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public void update(
      String grantName,
      String grantorName,
      String grantNumber,
      String description,
      BigDecimal awardAmount,
      LocalDate applicationDeadline,
      LocalDate startDate,
      LocalDate endDate,
      LocalDate reportingDeadline,
      Boolean restricted,
      String restrictionDescription,
      String primaryContactName,
      String primaryContactEmail,
      String notes) {
    if (status == GrantStatus.CLOSED) {
      throw new BusinessRuleException("CLOSED_GRANT_IMMUTABLE", "Closed grants cannot be edited");
    }
    if (grantName != null) this.grantName = grantName;
    if (grantorName != null) this.grantorName = grantorName;
    if (grantNumber != null) this.grantNumber = grantNumber;
    if (description != null) this.description = description;
    if (awardAmount != null) this.awardAmount = awardAmount;
    if (applicationDeadline != null) this.applicationDeadline = applicationDeadline;
    if (startDate != null) this.startDate = startDate;
    if (endDate != null) this.endDate = endDate;
    if (reportingDeadline != null) this.reportingDeadline = reportingDeadline;
    if (restricted != null) this.restricted = restricted;
    if (restrictionDescription != null) this.restrictionDescription = restrictionDescription;
    if (primaryContactName != null) this.primaryContactName = primaryContactName;
    if (primaryContactEmail != null) this.primaryContactEmail = primaryContactEmail;
    if (notes != null) this.notes = notes;
  }

  public void transition(GrantStatus target, LocalDate effectiveDate) {
    boolean allowed =
        switch (status) {
          case PROSPECT ->
              target == GrantStatus.APPLICATION_IN_PROGRESS || target == GrantStatus.WITHDRAWN;
          case APPLICATION_IN_PROGRESS ->
              target == GrantStatus.SUBMITTED || target == GrantStatus.WITHDRAWN;
          case SUBMITTED -> target == GrantStatus.AWARDED || target == GrantStatus.REJECTED;
          case AWARDED -> target == GrantStatus.ACTIVE;
          case ACTIVE -> target == GrantStatus.CLOSED;
          case CLOSED, REJECTED, WITHDRAWN -> false;
        };
    if (!allowed)
      throw new BusinessRuleException(
          "INVALID_GRANT_TRANSITION", "Grant cannot transition from " + status + " to " + target);
    status = target;
    if (target == GrantStatus.SUBMITTED && submittedDate == null) submittedDate = effectiveDate;
    if (target == GrantStatus.AWARDED && awardDate == null) awardDate = effectiveDate;
  }
}
