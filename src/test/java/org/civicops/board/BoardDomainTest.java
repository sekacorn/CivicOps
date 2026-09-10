package org.civicops.board;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.*;
import java.util.UUID;
import org.civicops.board.agenda.*;
import org.civicops.board.committee.*;
import org.civicops.board.meeting.*;
import org.civicops.board.member.*;
import org.civicops.board.minutes.*;
import org.civicops.board.motion.*;
import org.civicops.board.resolution.*;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class BoardDomainTest {
  Organization org = mock(Organization.class);
  User user = mock(User.class);
  LocalDate today = LocalDate.of(2030, 1, 1);

  BoardMember member() {
    return new BoardMember(
        org, null, "Ada", "Lovelace", "ADA@Example.org", null, "Director", today, null);
  }

  @Test
  void externalBoardMemberNormalizesEmailAndPreservesPrivacySummary() {
    BoardMember m = member();
    assertThat(m.getNormalizedEmail()).isEqualTo("ada@example.org");
    assertThat(BoardMemberDtos.Summary.from(m).toString()).doesNotContain("ADA@Example.org");
    m.deactivate();
    assertThat(m.isActive()).isFalse();
  }

  @Test
  void boardTermRequiresValidDatesAndHasTerminalLifecycle() {
    assertThatThrownBy(() -> new BoardTerm(member(), today, today))
        .isInstanceOf(BusinessRuleException.class);
    BoardTerm t = new BoardTerm(member(), today, today.plusYears(1));
    assertThat(t.eligibleOn(today.plusMonths(1))).isTrue();
    t.end(BoardTermStatus.RESIGNED);
    assertThatThrownBy(() -> t.end(BoardTermStatus.COMPLETED))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void officerOtherRequiresTitleAndEndDateIsValidated() {
    assertThatThrownBy(
            () -> new BoardOfficerAssignment(member(), BoardOfficerRole.OTHER, null, today, null))
        .isInstanceOf(BusinessRuleException.class);
    BoardOfficerAssignment a =
        new BoardOfficerAssignment(member(), BoardOfficerRole.CHAIR, null, today, null);
    assertThatThrownBy(() -> a.end(today.minusDays(1))).isInstanceOf(BusinessRuleException.class);
    a.end(today.plusDays(1));
    assertThat(a.isActive()).isFalse();
  }

  @Test
  void committeeMembershipEndsWithoutDeletion() {
    BoardCommittee c = new BoardCommittee(org, "Finance", null);
    BoardCommitteeMembership m = new BoardCommitteeMembership(c, member(), "Member", today);
    m.end(today.plusDays(2));
    assertThat(m.isActive()).isFalse();
    assertThat(m.getEndDate()).isEqualTo(today.plusDays(2));
  }

  @Test
  void meetingValidatesDatesQuorumAndLifecycle() {
    assertThatThrownBy(
            () ->
                new BoardMeeting(
                    org,
                    user,
                    "Meeting",
                    BoardMeetingType.REGULAR,
                    Instant.EPOCH,
                    Instant.EPOCH,
                    null,
                    null,
                    1))
        .isInstanceOf(BusinessRuleException.class);
    BoardMeeting m =
        new BoardMeeting(
            org,
            user,
            "Meeting",
            BoardMeetingType.REGULAR,
            Instant.EPOCH,
            Instant.EPOCH.plusSeconds(3600),
            null,
            null,
            1);
    m.transition(BoardMeetingStatus.PUBLISHED);
    m.transition(BoardMeetingStatus.IN_PROGRESS);
    m.transition(BoardMeetingStatus.COMPLETED);
    assertThatThrownBy(() -> m.update("Changed", null, null, null, null, null, null))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void attendanceOnlyPresentAndRemoteCountForQuorum() {
    BoardMeeting meeting =
        new BoardMeeting(
            org,
            user,
            "Meeting",
            BoardMeetingType.REGULAR,
            Instant.EPOCH,
            Instant.EPOCH.plusSeconds(1),
            null,
            null,
            1);
    assertThat(
            new BoardMeetingAttendance(
                    meeting, member(), AttendanceStatus.PRESENT, Instant.EPOCH, null)
                .countsForQuorum())
        .isTrue();
    assertThat(
            new BoardMeetingAttendance(
                    meeting, member(), AttendanceStatus.REMOTE, Instant.EPOCH, null)
                .countsForQuorum())
        .isTrue();
    assertThat(
            new BoardMeetingAttendance(meeting, member(), AttendanceStatus.EXCUSED, null, null)
                .countsForQuorum())
        .isFalse();
  }

  @Test
  void agendaLifecycleIsExplicit() {
    BoardMeeting meeting =
        new BoardMeeting(
            org,
            user,
            "Meeting",
            BoardMeetingType.REGULAR,
            Instant.EPOCH,
            Instant.EPOCH.plusSeconds(1),
            null,
            null,
            1);
    BoardAgendaItem a =
        new BoardAgendaItem(meeting, 1, "Call to order", null, AgendaItemType.ACTION, null, 5);
    a.transition(AgendaItemStatus.IN_PROGRESS);
    a.transition(AgendaItemStatus.COMPLETED);
    assertThatThrownBy(() -> a.transition(AgendaItemStatus.SKIPPED))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void motionRequiresDistinctSeconderAndUsesExplicitLifecycle() {
    BoardMeeting meeting = mock(BoardMeeting.class);
    when(meeting.getOrganization()).thenReturn(org);
    BoardMember mover = mock(BoardMember.class), seconder = mock(BoardMember.class);
    when(mover.getId()).thenReturn(UUID.randomUUID());
    when(seconder.getId()).thenReturn(UUID.randomUUID());
    BoardMotion motion = new BoardMotion(meeting, null, "Approve budget", mover, Instant.EPOCH);
    assertThatThrownBy(() -> motion.second(mover)).isInstanceOf(BusinessRuleException.class);
    motion.second(seconder);
    motion.openVoting();
    motion.close(true, Instant.EPOCH.plusSeconds(1));
    assertThat(motion.getStatus()).isEqualTo(MotionStatus.PASSED);
  }

  @Test
  void motionWithdrawalAndTablingAreTerminalAlternatives() {
    BoardMeeting meeting = mock(BoardMeeting.class);
    when(meeting.getOrganization()).thenReturn(org);
    BoardMember a = mock(BoardMember.class), b = mock(BoardMember.class);
    when(a.getId()).thenReturn(UUID.randomUUID());
    when(b.getId()).thenReturn(UUID.randomUUID());
    BoardMotion withdrawn = new BoardMotion(meeting, null, "Withdraw", a, Instant.EPOCH);
    withdrawn.withdraw(Instant.EPOCH);
    assertThat(withdrawn.getStatus()).isEqualTo(MotionStatus.WITHDRAWN);
    BoardMotion tabled = new BoardMotion(meeting, null, "Table", a, Instant.EPOCH);
    tabled.second(b);
    tabled.table(Instant.EPOCH);
    assertThat(tabled.getStatus()).isEqualTo(MotionStatus.TABLED);
  }

  @Test
  void approvedMinutesAreImmutable() {
    BoardMeeting meeting = mock(BoardMeeting.class);
    when(meeting.getOrganization()).thenReturn(org);
    BoardMeetingMinutes m = new BoardMeetingMinutes(meeting, "Draft", user);
    m.submit(Instant.EPOCH);
    m.approve(user, Instant.EPOCH.plusSeconds(1));
    assertThatThrownBy(() -> m.update("Changed")).isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void resolutionRescissionPreservesRecord() {
    BoardMotion motion = mock(BoardMotion.class);
    BoardMeeting meeting = mock(BoardMeeting.class);
    when(motion.getOrganization()).thenReturn(org);
    when(motion.getMeeting()).thenReturn(meeting);
    BoardResolution r = new BoardResolution(motion, "2030-1", "Budget", "Resolved", today, user);
    r.rescind(Instant.EPOCH);
    assertThat(r.getStatus()).isEqualTo(ResolutionStatus.RESCINDED);
    assertThatThrownBy(() -> r.rescind(Instant.EPOCH)).isInstanceOf(BusinessRuleException.class);
  }
}
