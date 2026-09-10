package org.civicops.board.meeting;

import jakarta.persistence.*;
import java.time.Instant;
import org.civicops.board.member.BoardMember;
import org.civicops.core.organization.Organization;
import org.civicops.shared.domain.BaseEntity;

@Entity
@Table(name = "board_meeting_attendance")
public class BoardMeetingAttendance extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "meeting_id")
  private BoardMeeting meeting;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "board_member_id")
  private BoardMember boardMember;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AttendanceStatus attendanceStatus;

  private Instant checkedInAt;

  @Column(columnDefinition = "TEXT")
  private String notes;

  protected BoardMeetingAttendance() {}

  public BoardMeetingAttendance(
      BoardMeeting meeting,
      BoardMember member,
      AttendanceStatus status,
      Instant checked,
      String notes) {
    organization = meeting.getOrganization();
    this.meeting = meeting;
    boardMember = member;
    update(status, checked, notes);
  }

  public void update(AttendanceStatus status, Instant checked, String notes) {
    if (status != null) {
      attendanceStatus = status;
      checkedInAt =
          (status == AttendanceStatus.PRESENT || status == AttendanceStatus.REMOTE)
              ? checked
              : null;
    }
    if (notes != null) this.notes = notes.isBlank() ? null : notes.trim();
  }

  public boolean countsForQuorum() {
    return attendanceStatus == AttendanceStatus.PRESENT
        || attendanceStatus == AttendanceStatus.REMOTE;
  }

  public Organization getOrganization() {
    return organization;
  }

  public BoardMeeting getMeeting() {
    return meeting;
  }

  public BoardMember getBoardMember() {
    return boardMember;
  }

  public AttendanceStatus getAttendanceStatus() {
    return attendanceStatus;
  }

  public Instant getCheckedInAt() {
    return checkedInAt;
  }

  public String getNotes() {
    return notes;
  }
}
