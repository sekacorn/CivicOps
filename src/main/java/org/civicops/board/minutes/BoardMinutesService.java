package org.civicops.board.minutes;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.civicops.board.meeting.BoardMeeting;
import org.civicops.board.meeting.BoardMeetingService;
import org.civicops.board.meeting.BoardMeetingStatus;
import org.civicops.core.user.UserService;
import org.civicops.shared.exception.BusinessRuleException;
import org.civicops.shared.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardMinutesService {
  private final BoardMeetingMinutesRepository minutes;
  private final BoardMeetingService meetings;
  private final UserService users;
  private final Clock clock;

  public BoardMinutesService(
      BoardMeetingMinutesRepository minutes,
      BoardMeetingService meetings,
      UserService users,
      Clock clock) {
    this.minutes = minutes;
    this.meetings = meetings;
    this.users = users;
    this.clock = clock;
  }

  @Transactional
  public BoardMinutesDtos.Response put(
      UUID organizationId, UUID meetingId, UUID actorId, String content) {
    BoardMeeting meeting = meetings.require(organizationId, meetingId);
    if (meeting.getStatus() == BoardMeetingStatus.CANCELLED) {
      throw new BusinessRuleException(
          "CANCELLED_MEETING_MINUTES", "Cancelled meetings cannot receive minutes");
    }

    BoardMeetingMinutes value =
        minutes
            .findByOrganizationIdAndMeetingId(organizationId, meetingId)
            .map(
                existing -> {
                  existing.update(content);
                  return existing;
                })
            .orElseGet(
                () ->
                    minutes.save(
                        new BoardMeetingMinutes(meeting, content, users.requireEntity(actorId))));
    return BoardMinutesDtos.Response.from(value);
  }

  @Transactional
  public BoardMinutesDtos.Response submit(UUID organizationId, UUID meetingId) {
    BoardMeetingMinutes value = require(organizationId, meetingId);
    value.submit(Instant.now(clock));
    return BoardMinutesDtos.Response.from(value);
  }

  @Transactional
  public BoardMinutesDtos.Response approve(UUID organizationId, UUID meetingId, UUID actorId) {
    BoardMeetingMinutes value = require(organizationId, meetingId);
    value.approve(users.requireEntity(actorId), Instant.now(clock));
    return BoardMinutesDtos.Response.from(value);
  }

  @Transactional(readOnly = true)
  public BoardMeetingMinutes require(UUID organizationId, UUID meetingId) {
    meetings.require(organizationId, meetingId);
    return minutes
        .findByOrganizationIdAndMeetingId(organizationId, meetingId)
        .orElseThrow(() -> new ResourceNotFoundException("Board meeting minutes", meetingId));
  }

  @Transactional(readOnly = true)
  public BoardMinutesDtos.Response get(UUID organizationId, UUID meetingId, boolean manager) {
    BoardMeetingMinutes value = require(organizationId, meetingId);
    if (!manager && value.getStatus() != MinutesStatus.APPROVED) {
      throw new AccessDeniedException("Board members may read only approved minutes");
    }
    return BoardMinutesDtos.Response.from(value);
  }
}
