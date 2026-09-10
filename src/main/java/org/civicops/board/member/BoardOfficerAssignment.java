package org.civicops.board.member;

import jakarta.persistence.*;
import java.time.LocalDate;
import org.civicops.core.organization.Organization;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "board_officer_assignment")
public class BoardOfficerAssignment extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "board_member_id")
  private BoardMember boardMember;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BoardOfficerRole officerRole;

  private String otherTitle;

  @Column(nullable = false)
  private LocalDate startDate;

  private LocalDate endDate;

  @Column(nullable = false)
  private boolean active = true;

  protected BoardOfficerAssignment() {}

  public BoardOfficerAssignment(
      BoardMember m, BoardOfficerRole role, String other, LocalDate start, LocalDate end) {
    organization = m.getOrganization();
    boardMember = m;
    officerRole = role;
    otherTitle = clean(other);
    startDate = start;
    endDate = end;
    validate();
  }

  private void validate() {
    if (officerRole == null || startDate == null)
      throw new BusinessRuleException(
          "INVALID_OFFICER_ASSIGNMENT", "Officer role and start date are required");
    if (officerRole == BoardOfficerRole.OTHER && otherTitle == null)
      throw new BusinessRuleException(
          "OFFICER_TITLE_REQUIRED", "Other officer role requires a title");
    if (endDate != null && endDate.isBefore(startDate))
      throw new BusinessRuleException(
          "INVALID_OFFICER_DATES", "Officer end date cannot precede start date");
  }

  public void end(LocalDate date) {
    if (!active)
      throw new BusinessRuleException(
          "OFFICER_ASSIGNMENT_ENDED", "Officer assignment is already ended");
    if (date == null || date.isBefore(startDate))
      throw new BusinessRuleException(
          "INVALID_OFFICER_DATES", "Officer end date cannot precede start date");
    endDate = date;
    active = false;
  }

  private static String clean(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  public Organization getOrganization() {
    return organization;
  }

  public BoardMember getBoardMember() {
    return boardMember;
  }

  public BoardOfficerRole getOfficerRole() {
    return officerRole;
  }

  public String getOtherTitle() {
    return otherTitle;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public boolean isActive() {
    return active;
  }
}
