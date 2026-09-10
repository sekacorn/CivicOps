package org.civicops.board.meeting;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.*;
import org.civicops.board.security.BoardAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/board-meetings")
@Tag(name = "Board Meetings")
public class BoardMeetingController {
  private final BoardMeetingService meetings;
  private final BoardAccessService access;

  public BoardMeetingController(BoardMeetingService m, BoardAccessService a) {
    meetings = m;
    access = a;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BoardMeetingDtos.Response create(
      @PathVariable UUID organizationId, @Valid @RequestBody BoardMeetingDtos.Create r) {
    access.requireBoardManagement(organizationId);
    return meetings.create(organizationId, access.userId(), r);
  }

  @GetMapping
  public Page<BoardMeetingDtos.Response> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) BoardMeetingStatus status,
      @RequestParam(required = false) BoardMeetingType meetingType,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to,
      @PageableDefault(size = 20, sort = "startDateTime") Pageable p) {
    access.requireBoardRead(organizationId);
    return meetings.list(
        organizationId,
        status,
        meetingType,
        from,
        to,
        SafePageables.allow(p, Set.of("startDateTime", "status", "createdAt")));
  }

  @GetMapping("/{meetingId}")
  public BoardMeetingDtos.Response detail(
      @PathVariable UUID organizationId, @PathVariable UUID meetingId) {
    access.requireBoardRead(organizationId);
    return meetings.detail(organizationId, meetingId);
  }

  @PatchMapping("/{meetingId}")
  public BoardMeetingDtos.Response update(
      @PathVariable UUID organizationId,
      @PathVariable UUID meetingId,
      @Valid @RequestBody BoardMeetingDtos.Update r) {
    access.requireBoardManagement(organizationId);
    return meetings.update(organizationId, meetingId, r);
  }

  @PostMapping("/{meetingId}/publish")
  public BoardMeetingDtos.Response publish(
      @PathVariable UUID organizationId, @PathVariable UUID meetingId) {
    return transition(organizationId, meetingId, BoardMeetingStatus.PUBLISHED);
  }

  @PostMapping("/{meetingId}/start")
  public BoardMeetingDtos.Response start(
      @PathVariable UUID organizationId, @PathVariable UUID meetingId) {
    return transition(organizationId, meetingId, BoardMeetingStatus.IN_PROGRESS);
  }

  @PostMapping("/{meetingId}/complete")
  public BoardMeetingDtos.Response complete(
      @PathVariable UUID organizationId, @PathVariable UUID meetingId) {
    return transition(organizationId, meetingId, BoardMeetingStatus.COMPLETED);
  }

  @PostMapping("/{meetingId}/cancel")
  public BoardMeetingDtos.Response cancel(
      @PathVariable UUID organizationId, @PathVariable UUID meetingId) {
    return transition(organizationId, meetingId, BoardMeetingStatus.CANCELLED);
  }

  @PostMapping("/{meetingId}/attendance")
  @ResponseStatus(HttpStatus.CREATED)
  public BoardMeetingDtos.AttendanceResponse attendance(
      @PathVariable UUID organizationId,
      @PathVariable UUID meetingId,
      @Valid @RequestBody BoardMeetingDtos.AttendanceRequest r) {
    access.requireBoardManagement(organizationId);
    return meetings.recordAttendance(organizationId, meetingId, r);
  }

  @GetMapping("/{meetingId}/attendance")
  public List<BoardMeetingDtos.AttendanceResponse> attendance(
      @PathVariable UUID organizationId, @PathVariable UUID meetingId) {
    access.requireBoardManagement(organizationId);
    return meetings.attendance(organizationId, meetingId);
  }

  @PutMapping("/{meetingId}/attendance/{memberId}")
  public BoardMeetingDtos.AttendanceResponse updateAttendance(
      @PathVariable UUID organizationId,
      @PathVariable UUID meetingId,
      @PathVariable UUID memberId,
      @Valid @RequestBody BoardMeetingDtos.AttendanceUpdate r) {
    access.requireBoardManagement(organizationId);
    return meetings.updateAttendance(organizationId, meetingId, memberId, r);
  }

  @PutMapping("/{meetingId}/attendance/me")
  public BoardMeetingDtos.AttendanceResponse selfAttendance(
      @PathVariable UUID organizationId,
      @PathVariable UUID meetingId,
      @Valid @RequestBody BoardMeetingDtos.AttendanceUpdate r) {
    access.requireBoardMemberSelfAccess(organizationId);
    return meetings.selfAttendance(organizationId, meetingId, access.userId(), r);
  }

  @GetMapping("/{meetingId}/quorum")
  public BoardMeetingDtos.QuorumResponse quorum(
      @PathVariable UUID organizationId, @PathVariable UUID meetingId) {
    access.requireBoardRead(organizationId);
    return meetings.quorum(organizationId, meetingId);
  }

  private BoardMeetingDtos.Response transition(UUID org, UUID id, BoardMeetingStatus target) {
    access.requireBoardManagement(org);
    return meetings.transition(org, id, target);
  }
}
