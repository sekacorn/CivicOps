package org.civicops.board.agenda;

import java.util.*;
import org.civicops.board.meeting.*;
import org.civicops.shared.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardAgendaService {
  private final BoardAgendaItemRepository items;
  private final BoardMeetingService meetings;

  public BoardAgendaService(BoardAgendaItemRepository i, BoardMeetingService m) {
    items = i;
    meetings = m;
  }

  @Transactional
  public BoardAgendaDtos.Response create(UUID org, UUID meetingId, BoardAgendaDtos.Create r) {
    BoardMeeting m = meetings.require(org, meetingId);
    mutable(m);
    if (items.existsByMeetingIdAndSequenceNumber(meetingId, r.sequenceNumber()))
      throw new ConflictException("DUPLICATE_AGENDA_SEQUENCE", "Agenda sequence already exists");
    return BoardAgendaDtos.Response.from(
        items.save(
            new BoardAgendaItem(
                m,
                r.sequenceNumber(),
                r.title(),
                r.description(),
                r.itemType(),
                r.presenter(),
                r.estimatedMinutes())));
  }

  @Transactional
  public BoardAgendaDtos.Response update(UUID org, UUID id, BoardAgendaDtos.Update r) {
    BoardAgendaItem a = require(org, id);
    mutable(a.getMeeting());
    if (r.sequenceNumber() != null
        && r.sequenceNumber() != a.getSequenceNumber()
        && items.existsByMeetingIdAndSequenceNumber(a.getMeeting().getId(), r.sequenceNumber()))
      throw new ConflictException("DUPLICATE_AGENDA_SEQUENCE", "Agenda sequence already exists");
    a.update(
        r.sequenceNumber(),
        r.title(),
        r.description(),
        r.itemType(),
        r.presenter(),
        r.estimatedMinutes());
    return BoardAgendaDtos.Response.from(a);
  }

  @Transactional
  public BoardAgendaDtos.Response transition(UUID org, UUID id, AgendaItemStatus status) {
    BoardAgendaItem a = require(org, id);
    if (a.getMeeting().getStatus() != BoardMeetingStatus.IN_PROGRESS)
      throw new BusinessRuleException(
          "MEETING_NOT_IN_PROGRESS", "Agenda lifecycle requires an in-progress meeting");
    a.transition(status);
    return BoardAgendaDtos.Response.from(a);
  }

  @Transactional(readOnly = true)
  public BoardAgendaItem require(UUID org, UUID id) {
    return items
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Board agenda item", id));
  }

  @Transactional(readOnly = true)
  public List<BoardAgendaDtos.Response> list(UUID org, UUID meeting) {
    meetings.require(org, meeting);
    return items.findAllByOrganizationIdAndMeetingIdOrderBySequenceNumber(org, meeting).stream()
        .map(BoardAgendaDtos.Response::from)
        .toList();
  }

  private static void mutable(BoardMeeting m) {
    if (m.getStatus() == BoardMeetingStatus.COMPLETED
        || m.getStatus() == BoardMeetingStatus.CANCELLED)
      throw new BusinessRuleException(
          "TERMINAL_MEETING_IMMUTABLE", "Terminal meeting agenda cannot change");
  }
}
