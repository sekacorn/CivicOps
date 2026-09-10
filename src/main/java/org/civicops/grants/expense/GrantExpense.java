package org.civicops.grants.expense;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.grants.grant.Grant;
import org.civicops.shared.domain.BaseEntity;

@Entity
@Table(name = "grant_expense")
public class GrantExpense extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "grant_id", nullable = false)
  private Grant grant;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false)
  private LocalDate expenseDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private ExpenseCategory category;

  @Column(nullable = false, length = 500)
  private String description;

  @Column(length = 200)
  private String vendor;

  @Column(length = 100)
  private String referenceNumber;

  @Column(columnDefinition = "TEXT")
  private String notes;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by_user_id", nullable = false, updatable = false)
  private User createdBy;

  protected GrantExpense() {}

  public GrantExpense(
      Organization organization,
      Grant grant,
      User createdBy,
      BigDecimal amount,
      LocalDate expenseDate,
      ExpenseCategory category,
      String description,
      String vendor,
      String referenceNumber,
      String notes) {
    this.organization = organization;
    this.grant = grant;
    this.createdBy = createdBy;
    this.amount = amount;
    this.expenseDate = expenseDate;
    this.category = category;
    this.description = description;
    this.vendor = vendor;
    this.referenceNumber = referenceNumber;
    this.notes = notes;
  }

  public Organization getOrganization() {
    return organization;
  }

  public Grant getGrant() {
    return grant;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public LocalDate getExpenseDate() {
    return expenseDate;
  }

  public ExpenseCategory getCategory() {
    return category;
  }

  public String getDescription() {
    return description;
  }

  public String getVendor() {
    return vendor;
  }

  public String getReferenceNumber() {
    return referenceNumber;
  }

  public String getNotes() {
    return notes;
  }

  public User getCreatedBy() {
    return createdBy;
  }
}
