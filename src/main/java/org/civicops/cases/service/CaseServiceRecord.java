package org.civicops.cases.service;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.civicops.cases.casefile.CaseRecord;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.shared.domain.BaseEntity;

@Entity
@Table(name = "case_service_record")
public class CaseServiceRecord extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "case_id", nullable = false)
  private CaseRecord caseRecord;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private CaseServiceType serviceType;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false)
  private LocalDate serviceDate;

  @Column(precision = 19, scale = 2)
  private BigDecimal quantity;

  @Column(length = 50)
  private String unit;

  @Column(precision = 19, scale = 2)
  private BigDecimal valueAmount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "provided_by_user_id")
  private User providedBy;

  @Column(columnDefinition = "TEXT")
  private String notes;

  protected CaseServiceRecord() {}

  public CaseServiceRecord(
      Organization o,
      CaseRecord c,
      CaseServiceType type,
      String description,
      LocalDate date,
      BigDecimal quantity,
      String unit,
      BigDecimal value,
      User provider,
      String notes) {
    organization = o;
    caseRecord = c;
    serviceType = type;
    this.description = description;
    serviceDate = date;
    this.quantity = quantity;
    this.unit = unit;
    valueAmount = value;
    providedBy = provider;
    this.notes = notes;
  }

  public Organization getOrganization() {
    return organization;
  }

  public CaseRecord getCaseRecord() {
    return caseRecord;
  }

  public CaseServiceType getServiceType() {
    return serviceType;
  }

  public String getDescription() {
    return description;
  }

  public LocalDate getServiceDate() {
    return serviceDate;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public String getUnit() {
    return unit;
  }

  public BigDecimal getValueAmount() {
    return valueAmount;
  }

  public User getProvidedBy() {
    return providedBy;
  }

  public String getNotes() {
    return notes;
  }
}
