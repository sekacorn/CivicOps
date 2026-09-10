package org.civicops.board.member;

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
@Tag(name = "Board Members")
public class BoardMemberController {
  private final BoardMemberService members;
  private final BoardAccessService access;

  public BoardMemberController(BoardMemberService m, BoardAccessService a) {
    members = m;
    access = a;
  }

  @PostMapping("/board-members")
  @ResponseStatus(HttpStatus.CREATED)
  public BoardMemberDtos.Detail create(
      @PathVariable UUID organizationId, @Valid @RequestBody BoardMemberDtos.Create r) {
    access.requireBoardManagement(organizationId);
    return members.create(organizationId, r);
  }

  @GetMapping("/board-members")
  public Page<BoardMemberDtos.Summary> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) Boolean active,
      @RequestParam(required = false) BoardOfficerRole officerRole,
      @RequestParam(required = false) BoardTermStatus termStatus,
      @PageableDefault(size = 20, sort = "lastName") Pageable p) {
    access.requireBoardRead(organizationId);
    return members.list(
        organizationId,
        active,
        officerRole,
        termStatus,
        SafePageables.allow(p, Set.of("lastName", "joinedDate", "createdAt")));
  }

  @GetMapping("/board-members/{memberId}")
  public BoardMemberDtos.Detail detail(
      @PathVariable UUID organizationId, @PathVariable UUID memberId) {
    access.requireBoardManagement(organizationId);
    return members.detail(organizationId, memberId);
  }

  @PatchMapping("/board-members/{memberId}")
  public BoardMemberDtos.Detail update(
      @PathVariable UUID organizationId,
      @PathVariable UUID memberId,
      @Valid @RequestBody BoardMemberDtos.Update r) {
    access.requireBoardManagement(organizationId);
    return members.update(organizationId, memberId, r);
  }

  @PostMapping("/board-members/{memberId}/deactivate")
  public BoardMemberDtos.Detail deactivate(
      @PathVariable UUID organizationId, @PathVariable UUID memberId) {
    access.requireBoardManagement(organizationId);
    return members.active(organizationId, memberId, false);
  }

  @PostMapping("/board-members/{memberId}/terms")
  @ResponseStatus(HttpStatus.CREATED)
  public BoardMemberDtos.TermResponse term(
      @PathVariable UUID organizationId,
      @PathVariable UUID memberId,
      @Valid @RequestBody BoardMemberDtos.CreateTerm r) {
    access.requireBoardManagement(organizationId);
    return members.addTerm(organizationId, memberId, r);
  }

  @GetMapping("/board-members/{memberId}/terms")
  public List<BoardMemberDtos.TermResponse> terms(
      @PathVariable UUID organizationId, @PathVariable UUID memberId) {
    access.requireBoardRead(organizationId);
    return members.terms(organizationId, memberId);
  }

  @PostMapping("/board-terms/{termId}/end")
  public BoardMemberDtos.TermResponse endTerm(
      @PathVariable UUID organizationId,
      @PathVariable UUID termId,
      @Valid @RequestBody BoardMemberDtos.EndTerm r) {
    access.requireBoardManagement(organizationId);
    return members.endTerm(organizationId, termId, r.status());
  }

  @PostMapping("/board-members/{memberId}/officer-assignments")
  @ResponseStatus(HttpStatus.CREATED)
  public BoardMemberDtos.OfficerResponse officer(
      @PathVariable UUID organizationId,
      @PathVariable UUID memberId,
      @Valid @RequestBody BoardMemberDtos.CreateOfficer r) {
    access.requireBoardManagement(organizationId);
    return members.addOfficer(organizationId, memberId, r);
  }

  @GetMapping("/board-members/{memberId}/officer-assignments")
  public List<BoardMemberDtos.OfficerResponse> officers(
      @PathVariable UUID organizationId, @PathVariable UUID memberId) {
    access.requireBoardRead(organizationId);
    return members.officers(organizationId, memberId);
  }

  @PostMapping("/board-officer-assignments/{assignmentId}/end")
  public BoardMemberDtos.OfficerResponse endOfficer(
      @PathVariable UUID organizationId,
      @PathVariable UUID assignmentId,
      @Valid @RequestBody BoardMemberDtos.EndOfficer r) {
    access.requireBoardManagement(organizationId);
    return members.endOfficer(organizationId, assignmentId, r.endDate());
  }
}
