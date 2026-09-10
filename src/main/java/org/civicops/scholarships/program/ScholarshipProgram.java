package org.civicops.scholarships.program;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "scholarship_program")
public class ScholarshipProgram extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(length = 30)
  private String academicYear;

  @Column(nullable = false)
  private LocalDate applicationOpenDate;

  @Column(nullable = false)
  private LocalDate applicationDeadline;

  @Column(precision = 19, scale = 2)
  private BigDecimal awardAmount;

  private Integer numberOfAwards;

  @Column(columnDefinition = "TEXT")
  private String eligibilityDescription;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ScholarshipProgramStatus status = ScholarshipProgramStatus.DRAFT;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by_user_id", nullable = false, updatable = false)
  private User createdBy;

  protected ScholarshipProgram() {}

  public ScholarshipProgram(
      Organization o,
      User u,
      String n,
      String d,
      String y,
      LocalDate open,
      LocalDate deadline,
      BigDecimal amount,
      Integer count,
      String eligibility) {
    organization = o;
    createdBy = u;
    name = n;
    description = d;
    academicYear = y;
    applicationOpenDate = open;
    applicationDeadline = deadline;
    awardAmount = amount;
    numberOfAwards = count;
    eligibilityDescription = eligibility;
    validate();
  }

  public void update(
      String n,
      String d,
      String y,
      LocalDate open,
      LocalDate deadline,
      BigDecimal amount,
      Integer count,
      String eligibility) {
    if (status == ScholarshipProgramStatus.AWARDED || status == ScholarshipProgramStatus.CANCELLED)
      throw new BusinessRuleException(
          "TERMINAL_PROGRAM_IMMUTABLE", "Terminal scholarship programs cannot be edited");
    if (n != null) name = n;
    if (d != null) description = d;
    if (y != null) academicYear = y;
    if (open != null) applicationOpenDate = open;
    if (deadline != null) applicationDeadline = deadline;
    if (amount != null) awardAmount = amount;
    if (count != null) numberOfAwards = count;
    if (eligibility != null) eligibilityDescription = eligibility;
    validate();
  }

  private void validate() {
    if (!applicationDeadline.isAfter(applicationOpenDate))
      throw new BusinessRuleException(
          "INVALID_PROGRAM_DATES", "Application deadline must be after open date");
    if (awardAmount != null && awardAmount.signum() < 0)
      throw new BusinessRuleException(
          "INVALID_AWARD_AMOUNT", "Program award amount cannot be negative");
    if (numberOfAwards != null && numberOfAwards <= 0)
      throw new BusinessRuleException("INVALID_AWARD_COUNT", "Number of awards must be positive");
  }

  public void transition(ScholarshipProgramStatus target) {
    boolean ok =
        switch (status) {
          case DRAFT ->
              target == ScholarshipProgramStatus.OPEN
                  || target == ScholarshipProgramStatus.CANCELLED;
          case OPEN ->
              target == ScholarshipProgramStatus.CLOSED
                  || target == ScholarshipProgramStatus.CANCELLED;
          case CLOSED ->
              target == ScholarshipProgramStatus.REVIEWING
                  || target == ScholarshipProgramStatus.CANCELLED;
          case REVIEWING ->
              target == ScholarshipProgramStatus.AWARDED
                  || target == ScholarshipProgramStatus.CANCELLED;
          case AWARDED, CANCELLED -> false;
        };
    if (!ok)
      throw new BusinessRuleException(
          "INVALID_PROGRAM_TRANSITION",
          "Program cannot transition from " + status + " to " + target);
    status = target;
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

  public String getAcademicYear() {
    return academicYear;
  }

  public LocalDate getApplicationOpenDate() {
    return applicationOpenDate;
  }

  public LocalDate getApplicationDeadline() {
    return applicationDeadline;
  }

  public BigDecimal getAwardAmount() {
    return awardAmount;
  }

  public Integer getNumberOfAwards() {
    return numberOfAwards;
  }

  public String getEligibilityDescription() {
    return eligibilityDescription;
  }

  public ScholarshipProgramStatus getStatus() {
    return status;
  }

  public User getCreatedBy() {
    return createdBy;
  }
}
