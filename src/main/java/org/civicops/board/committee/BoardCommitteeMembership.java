package org.civicops.board.committee;

import jakarta.persistence.*;
import java.time.LocalDate;
import org.civicops.board.member.BoardMember;
import org.civicops.core.organization.Organization;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "board_committee_membership")
public class BoardCommitteeMembership extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "committee_id")
  private BoardCommittee committee;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "board_member_id")
  private BoardMember boardMember;

  private String committeeRole;

  @Column(nullable = false)
  private LocalDate startDate;

  private LocalDate endDate;

  @Column(nullable = false)
  private boolean active = true;

  protected BoardCommitteeMembership() {}

  public BoardCommitteeMembership(BoardCommittee c, BoardMember m, String role, LocalDate start) {
    organization = c.getOrganization();
    committee = c;
    boardMember = m;
    committeeRole = clean(role);
    startDate = start;
    if (start == null)
      throw new BusinessRuleException("INVALID_COMMITTEE_MEMBERSHIP", "Start date is required");
  }

  public void end(LocalDate date) {
    if (!active)
      throw new BusinessRuleException(
          "COMMITTEE_MEMBERSHIP_ENDED", "Committee membership is already ended");
    if (date == null || date.isBefore(startDate))
      throw new BusinessRuleException(
          "INVALID_COMMITTEE_MEMBERSHIP", "End date cannot precede start date");
    endDate = date;
    active = false;
  }

  private static String clean(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  public Organization getOrganization() {
    return organization;
  }

  public BoardCommittee getCommittee() {
    return committee;
  }

  public BoardMember getBoardMember() {
    return boardMember;
  }

  public String getCommitteeRole() {
    return committeeRole;
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
