package org.civicops.scholarships.award;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.scholarships.applicant.ScholarshipApplicant;
import org.civicops.scholarships.application.ScholarshipApplication;
import org.civicops.scholarships.program.ScholarshipProgram;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "scholarship_award")
public class ScholarshipAward extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "scholarship_program_id", nullable = false)
  private ScholarshipProgram program;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "application_id", nullable = false, unique = true)
  private ScholarshipApplication application;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "applicant_id", nullable = false)
  private ScholarshipApplicant applicant;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false)
  private LocalDate awardDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ScholarshipAwardStatus status = ScholarshipAwardStatus.OFFERED;

  @Column(columnDefinition = "TEXT")
  private String notes;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by_user_id", nullable = false, updatable = false)
  private User createdBy;

  protected ScholarshipAward() {}

  public ScholarshipAward(
      ScholarshipApplication a, BigDecimal amount, LocalDate date, String notes, User by) {
    organization = a.getOrganization();
    program = a.getProgram();
    application = a;
    applicant = a.getApplicant();
    this.amount = amount;
    awardDate = date;
    this.notes = notes;
    createdBy = by;
    if (amount.signum() <= 0)
      throw new BusinessRuleException("INVALID_AWARD_AMOUNT", "Award amount must be positive");
  }

  public void transition(ScholarshipAwardStatus target) {
    boolean ok =
        switch (status) {
          case OFFERED ->
              target == ScholarshipAwardStatus.ACCEPTED
                  || target == ScholarshipAwardStatus.DECLINED
                  || target == ScholarshipAwardStatus.CANCELLED;
          case ACCEPTED ->
              target == ScholarshipAwardStatus.DISBURSED
                  || target == ScholarshipAwardStatus.CANCELLED;
          case DECLINED, DISBURSED, CANCELLED -> false;
        };
    if (!ok)
      throw new BusinessRuleException(
          "INVALID_AWARD_TRANSITION", "Award cannot transition from " + status + " to " + target);
    status = target;
  }

  public ScholarshipProgram getProgram() {
    return program;
  }

  public ScholarshipApplication getApplication() {
    return application;
  }

  public ScholarshipApplicant getApplicant() {
    return applicant;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public LocalDate getAwardDate() {
    return awardDate;
  }

  public ScholarshipAwardStatus getStatus() {
    return status;
  }

  public String getNotes() {
    return notes;
  }

  public User getCreatedBy() {
    return createdBy;
  }
}
