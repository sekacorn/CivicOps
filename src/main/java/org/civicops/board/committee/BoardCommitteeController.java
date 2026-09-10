package org.civicops.board.committee;

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
@Tag(name = "Board Committees")
public class BoardCommitteeController {
  private final BoardCommitteeService committees;
  private final BoardAccessService access;

  public BoardCommitteeController(BoardCommitteeService c, BoardAccessService a) {
    committees = c;
    access = a;
  }

  @PostMapping("/board-committees")
  @ResponseStatus(HttpStatus.CREATED)
  public BoardCommitteeDtos.Response create(
      @PathVariable UUID organizationId, @Valid @RequestBody BoardCommitteeDtos.Create r) {
    access.requireBoardManagement(organizationId);
    return committees.create(organizationId, r);
  }

  @GetMapping("/board-committees")
  public Page<BoardCommitteeDtos.Response> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) Boolean active,
      @PageableDefault(size = 20, sort = "name") Pageable p) {
    access.requireBoardRead(organizationId);
    return committees.list(
        organizationId, active, SafePageables.allow(p, Set.of("name", "createdAt")));
  }

  @GetMapping("/board-committees/{committeeId}")
  public BoardCommitteeDtos.Response detail(
      @PathVariable UUID organizationId, @PathVariable UUID committeeId) {
    access.requireBoardRead(organizationId);
    return BoardCommitteeDtos.Response.from(committees.require(organizationId, committeeId));
  }

  @PatchMapping("/board-committees/{committeeId}")
  public BoardCommitteeDtos.Response update(
      @PathVariable UUID organizationId,
      @PathVariable UUID committeeId,
      @Valid @RequestBody BoardCommitteeDtos.Update r) {
    access.requireBoardManagement(organizationId);
    return committees.update(organizationId, committeeId, r);
  }

  @PostMapping("/board-committees/{committeeId}/deactivate")
  public BoardCommitteeDtos.Response deactivate(
      @PathVariable UUID organizationId, @PathVariable UUID committeeId) {
    access.requireBoardManagement(organizationId);
    return committees.deactivate(organizationId, committeeId);
  }

  @PostMapping("/board-committees/{committeeId}/members")
  @ResponseStatus(HttpStatus.CREATED)
  public BoardCommitteeDtos.MembershipResponse member(
      @PathVariable UUID organizationId,
      @PathVariable UUID committeeId,
      @Valid @RequestBody BoardCommitteeDtos.AddMember r) {
    access.requireBoardManagement(organizationId);
    return committees.addMember(organizationId, committeeId, r);
  }

  @GetMapping("/board-committees/{committeeId}/members")
  public List<BoardCommitteeDtos.MembershipResponse> members(
      @PathVariable UUID organizationId, @PathVariable UUID committeeId) {
    access.requireBoardRead(organizationId);
    return committees.members(organizationId, committeeId);
  }

  @PostMapping("/board-committee-memberships/{membershipId}/end")
  public BoardCommitteeDtos.MembershipResponse end(
      @PathVariable UUID organizationId,
      @PathVariable UUID membershipId,
      @Valid @RequestBody BoardCommitteeDtos.EndMember r) {
    access.requireBoardManagement(organizationId);
    return committees.endMember(organizationId, membershipId, r.endDate());
  }
}
