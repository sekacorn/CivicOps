package org.civicops.board.member;

import jakarta.persistence.*;
import java.time.LocalDate;
import org.civicops.core.organization.Organization;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(name = "board_term")
public class BoardTerm extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "board_member_id")
  private BoardMember boardMember;

  @Column(nullable = false)
  private LocalDate termStart;

  @Column(nullable = false)
  private LocalDate termEnd;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BoardTermStatus status = BoardTermStatus.ACTIVE;

  protected BoardTerm() {}

  public BoardTerm(BoardMember m, LocalDate start, LocalDate end) {
    organization = m.getOrganization();
    boardMember = m;
    termStart = start;
    termEnd = end;
    if (start == null || end == null || !end.isAfter(start))
      throw new BusinessRuleException("INVALID_BOARD_TERM", "Term end must be after term start");
  }

  public void end(BoardTermStatus target) {
    if (status != BoardTermStatus.ACTIVE || target == BoardTermStatus.ACTIVE)
      throw new BusinessRuleException(
          "INVALID_BOARD_TERM_TRANSITION", "Only active terms may be ended");
    status = target;
  }

  public boolean eligibleOn(LocalDate date) {
    return status == BoardTermStatus.ACTIVE && !date.isBefore(termStart) && !date.isAfter(termEnd);
  }

  public Organization getOrganization() {
    return organization;
  }

  public BoardMember getBoardMember() {
    return boardMember;
  }

  public LocalDate getTermStart() {
    return termStart;
  }

  public LocalDate getTermEnd() {
    return termEnd;
  }

  public BoardTermStatus getStatus() {
    return status;
  }
}
