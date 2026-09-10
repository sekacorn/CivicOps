package org.civicops.scholarships.application;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.civicops.core.organization.Organization;
import org.civicops.scholarships.applicant.ScholarshipApplicant;
import org.civicops.scholarships.program.ScholarshipProgram;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "scholarship_application")
public class ScholarshipApplication extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "scholarship_program_id", nullable = false)
  private ScholarshipProgram program;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "applicant_id", nullable = false)
  private ScholarshipApplicant applicant;

  private Instant submittedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ScholarshipApplicationStatus status = ScholarshipApplicationStatus.DRAFT;

  @Column(nullable = false)
  private boolean eligibilityConfirmed;

  @Column(columnDefinition = "TEXT")
  private String eligibilityNotes;

  @Column(columnDefinition = "TEXT")
  private String personalStatement;

  @Column(columnDefinition = "TEXT")
  private String financialNeedStatement;

  @Column(precision = 4, scale = 2)
  private BigDecimal gpa;

  @Column(precision = 19, scale = 2)
  private BigDecimal householdIncome;

  @Column(precision = 19, scale = 2)
  private BigDecimal requestedAmount;

  protected ScholarshipApplication() {}

  public ScholarshipApplication(
      ScholarshipProgram p,
      ScholarshipApplicant a,
      boolean eligible,
      String eligibilityNotes,
      String statement,
      String need,
      BigDecimal gpa,
      BigDecimal income,
      BigDecimal requested) {
    organization = p.getOrganization();
    program = p;
    applicant = a;
    eligibilityConfirmed = eligible;
    this.eligibilityNotes = eligibilityNotes;
    personalStatement = statement;
    financialNeedStatement = need;
    this.gpa = gpa;
    householdIncome = income;
    requestedAmount = requested;
    validate();
  }

  public void update(
      Boolean eligible,
      String eligibilityNotes,
      String statement,
      String need,
      BigDecimal gpa,
      BigDecimal income,
      BigDecimal requested) {
    if (status != ScholarshipApplicationStatus.DRAFT)
      throw new BusinessRuleException(
          "SUBMITTED_APPLICATION_IMMUTABLE",
          "Submitted applications cannot be edited through generic PATCH");
    if (eligible != null) eligibilityConfirmed = eligible;
    if (eligibilityNotes != null) this.eligibilityNotes = eligibilityNotes;
    if (statement != null) personalStatement = statement;
    if (need != null) financialNeedStatement = need;
    if (gpa != null) this.gpa = gpa;
    if (income != null) householdIncome = income;
    if (requested != null) requestedAmount = requested;
    validate();
  }

  private void validate() {
    if (gpa != null && (gpa.signum() < 0 || gpa.compareTo(BigDecimal.valueOf(4)) > 0))
      throw new BusinessRuleException("INVALID_GPA", "GPA must be between 0.00 and 4.00");
    if (householdIncome != null && householdIncome.signum() < 0)
      throw new BusinessRuleException(
          "INVALID_HOUSEHOLD_INCOME", "Household income cannot be negative");
    if (requestedAmount != null && requestedAmount.signum() <= 0)
      throw new BusinessRuleException(
          "INVALID_REQUESTED_AMOUNT", "Requested amount must be positive");
  }

  public void submit(Instant now) {
    if (status != ScholarshipApplicationStatus.DRAFT) invalid();
    status = ScholarshipApplicationStatus.SUBMITTED;
    submittedAt = now;
  }

  public void startReview() {
    if (status != ScholarshipApplicationStatus.SUBMITTED) invalid();
    status = ScholarshipApplicationStatus.UNDER_REVIEW;
  }

  public void finalist() {
    if (status != ScholarshipApplicationStatus.UNDER_REVIEW) invalid();
    status = ScholarshipApplicationStatus.FINALIST;
  }

  public void select() {
    if (status != ScholarshipApplicationStatus.FINALIST) invalid();
    status = ScholarshipApplicationStatus.SELECTED;
  }

  public void notSelect() {
    if (status != ScholarshipApplicationStatus.UNDER_REVIEW
        && status != ScholarshipApplicationStatus.FINALIST) invalid();
    status = ScholarshipApplicationStatus.NOT_SELECTED;
  }

  public void withdraw(Instant now) {
    if (status != ScholarshipApplicationStatus.DRAFT
        && status != ScholarshipApplicationStatus.SUBMITTED) invalid();
    status = ScholarshipApplicationStatus.WITHDRAWN;
    if (submittedAt == null) submittedAt = now;
  }

  private void invalid() {
    throw new BusinessRuleException(
        "INVALID_APPLICATION_TRANSITION", "Application cannot transition from " + status);
  }

  public Organization getOrganization() {
    return organization;
  }

  public ScholarshipProgram getProgram() {
    return program;
  }

  public ScholarshipApplicant getApplicant() {
    return applicant;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public ScholarshipApplicationStatus getStatus() {
    return status;
  }

  public boolean isEligibilityConfirmed() {
    return eligibilityConfirmed;
  }

  public String getEligibilityNotes() {
    return eligibilityNotes;
  }

  public String getPersonalStatement() {
    return personalStatement;
  }

  public String getFinancialNeedStatement() {
    return financialNeedStatement;
  }

  public BigDecimal getGpa() {
    return gpa;
  }

  public BigDecimal getHouseholdIncome() {
    return householdIncome;
  }

  public BigDecimal getRequestedAmount() {
    return requestedAmount;
  }
}
