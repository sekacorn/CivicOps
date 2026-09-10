package org.civicops.board.motion;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.civicops.board.security.BoardAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
@Tag(name = "Board Motions and Votes")
public class BoardMotionController {
  private final BoardMotionService motions;
  private final BoardAccessService access;

  public BoardMotionController(BoardMotionService m, BoardAccessService a) {
    motions = m;
    access = a;
  }

  @PostMapping("/board-meetings/{meetingId}/motions")
  @ResponseStatus(HttpStatus.CREATED)
  public BoardMotionDtos.Response create(
      @PathVariable UUID organizationId,
      @PathVariable UUID meetingId,
      @Valid @RequestBody BoardMotionDtos.Create r) {
    access.requireBoardManagement(organizationId);
    return motions.create(organizationId, meetingId, r);
  }

  @GetMapping("/board-meetings/{meetingId}/motions")
  public Page<BoardMotionDtos.Response> meetingMotions(
      @PathVariable UUID organizationId,
      @PathVariable UUID meetingId,
      @RequestParam(required = false) MotionStatus status,
      @PageableDefault(size = 20, sort = "openedAt") Pageable p) {
    access.requireBoardRead(organizationId);
    return motions.list(
        organizationId,
        meetingId,
        status,
        SafePageables.allow(p, Set.of("openedAt", "status", "createdAt")));
  }

  @GetMapping("/board-motions/{motionId}")
  public BoardMotionDtos.Response detail(
      @PathVariable UUID organizationId, @PathVariable UUID motionId) {
    access.requireBoardRead(organizationId);
    return motions.detail(organizationId, motionId);
  }

  @PostMapping("/board-motions/{motionId}/second")
  public BoardMotionDtos.Response second(
      @PathVariable UUID organizationId,
      @PathVariable UUID motionId,
      @Valid @RequestBody BoardMotionDtos.Second r) {
    access.requireBoardManagement(organizationId);
    return motions.second(organizationId, motionId, r.secondedByBoardMemberId());
  }

  @PostMapping("/board-motions/{motionId}/open-voting")
  public BoardMotionDtos.Response open(
      @PathVariable UUID organizationId, @PathVariable UUID motionId) {
    access.requireBoardManagement(organizationId);
    return motions.open(organizationId, motionId);
  }

  @PostMapping("/board-motions/{motionId}/close-voting")
  public BoardMotionDtos.Response close(
      @PathVariable UUID organizationId, @PathVariable UUID motionId) {
    access.requireBoardManagement(organizationId);
    return motions.close(organizationId, motionId);
  }

  @PostMapping("/board-motions/{motionId}/withdraw")
  public BoardMotionDtos.Response withdraw(
      @PathVariable UUID organizationId, @PathVariable UUID motionId) {
    access.requireBoardManagement(organizationId);
    return motions.withdraw(organizationId, motionId);
  }

  @PostMapping("/board-motions/{motionId}/table")
  public BoardMotionDtos.Response table(
      @PathVariable UUID organizationId, @PathVariable UUID motionId) {
    access.requireBoardManagement(organizationId);
    return motions.table(organizationId, motionId);
  }

  @PostMapping("/board-motions/{motionId}/votes/me")
  @ResponseStatus(HttpStatus.CREATED)
  public BoardMotionDtos.VoteResponse vote(
      @PathVariable UUID organizationId,
      @PathVariable UUID motionId,
      @Valid @RequestBody BoardMotionDtos.VoteRequest r) {
    access.requireVotingAccess(organizationId);
    return motions.voteSelf(organizationId, motionId, access.userId(), r.choice());
  }

  @GetMapping("/board-motions/{motionId}/votes")
  public List<BoardMotionDtos.VoteResponse> votes(
      @PathVariable UUID organizationId, @PathVariable UUID motionId) {
    access.requireBoardManagement(organizationId);
    return motions.votes(organizationId, motionId);
  }
}
