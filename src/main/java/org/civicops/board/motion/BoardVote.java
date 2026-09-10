package org.civicops.board.motion;

import jakarta.persistence.*;
import java.time.Instant;
import org.civicops.board.member.BoardMember;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.shared.domain.BaseEntity;

@Entity
@Table(name = "board_vote")
public class BoardVote extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "motion_id")
  private BoardMotion motion;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "board_member_id")
  private BoardMember boardMember;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private VoteChoice choice;

  @Column(nullable = false)
  private Instant castAt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "recorded_by_user_id")
  private User recordedBy;

  protected BoardVote() {}

  public BoardVote(
      BoardMotion motion, BoardMember member, VoteChoice choice, Instant now, User recorder) {
    organization = motion.getOrganization();
    this.motion = motion;
    boardMember = member;
    this.choice = choice;
    castAt = now;
    recordedBy = recorder;
  }

  public Organization getOrganization() {
    return organization;
  }

  public BoardMotion getMotion() {
    return motion;
  }

  public BoardMember getBoardMember() {
    return boardMember;
  }

  public VoteChoice getChoice() {
    return choice;
  }

  public Instant getCastAt() {
    return castAt;
  }

  public User getRecordedBy() {
    return recordedBy;
  }
}
