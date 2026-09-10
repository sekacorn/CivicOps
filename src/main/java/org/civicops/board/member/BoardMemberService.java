package org.civicops.board.member;

import java.time.LocalDate;
import java.util.*;
import org.civicops.core.membership.OrganizationMembershipRepository;
import org.civicops.core.organization.OrganizationService;
import org.civicops.core.user.*;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardMemberService {
  private final BoardMemberRepository members;
  private final BoardTermRepository terms;
  private final BoardOfficerAssignmentRepository officers;
  private final OrganizationService organizations;
  private final UserService users;
  private final OrganizationMembershipRepository memberships;

  public BoardMemberService(
      BoardMemberRepository m,
      BoardTermRepository t,
      BoardOfficerAssignmentRepository o,
      OrganizationService orgs,
      UserService u,
      OrganizationMembershipRepository ms) {
    members = m;
    terms = t;
    officers = o;
    organizations = orgs;
    users = u;
    memberships = ms;
  }

  @Transactional
  public BoardMemberDtos.Detail create(UUID org, BoardMemberDtos.Create r) {
    User user = null;
    if (r.userId() != null) {
      if (members.existsByOrganizationIdAndUserId(org, r.userId()))
        throw new ConflictException(
            "DUPLICATE_BOARD_USER", "User already has a board-member identity");
      user = users.requireEntity(r.userId());
      if (!user.isActive()
          || memberships.findByOrganizationIdAndUserIdAndActiveTrue(org, r.userId()).isEmpty())
        throw new BusinessRuleException(
            "INVALID_BOARD_USER", "Linked user must be an active organization member");
    }
    return BoardMemberDtos.Detail.from(
        members.save(
            new BoardMember(
                organizations.requireEntity(org),
                user,
                r.firstName(),
                r.lastName(),
                r.email(),
                r.phone(),
                r.title(),
                r.joinedDate(),
                r.notes())));
  }

  @Transactional
  public BoardMemberDtos.Detail update(UUID org, UUID id, BoardMemberDtos.Update r) {
    BoardMember m = require(org, id);
    m.update(
        r.firstName(), r.lastName(), r.email(), r.phone(), r.title(), r.joinedDate(), r.notes());
    return BoardMemberDtos.Detail.from(m);
  }

  @Transactional
  public BoardMemberDtos.Detail active(UUID org, UUID id, boolean active) {
    BoardMember m = require(org, id);
    if (active) m.activate();
    else m.deactivate();
    return BoardMemberDtos.Detail.from(m);
  }

  @Transactional(readOnly = true)
  public BoardMember require(UUID org, UUID id) {
    return members
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Board member", id));
  }

  @Transactional(readOnly = true)
  public BoardMember linked(UUID org, UUID user) {
    return members
        .findByOrganizationIdAndUserIdAndActiveTrue(org, user)
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "BOARD_IDENTITY_REQUIRED",
                    "Authenticated user has no active board-member identity"));
  }

  @Transactional(readOnly = true)
  public void requireEligible(UUID org, BoardMember m, LocalDate date) {
    if (!m.isActive() || !terms.eligible(org, m.getId(), date))
      throw new BusinessRuleException(
          "BOARD_MEMBER_NOT_ELIGIBLE",
          "Board member must be active with an active term covering the meeting date");
  }

  @Transactional(readOnly = true)
  public BoardMemberDtos.Detail detail(UUID org, UUID id) {
    return BoardMemberDtos.Detail.from(require(org, id));
  }

  @Transactional(readOnly = true)
  public Page<BoardMemberDtos.Summary> list(
      UUID org, Boolean active, BoardOfficerRole officer, BoardTermStatus term, Pageable p) {
    Specification<BoardMember> s = (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (active != null) s = s.and((r, q, c) -> c.equal(r.get("active"), active));
    if (officer != null)
      s = s.and((r, q, c) -> r.get("id").in(officers.activeMemberIds(org, officer)));
    if (term != null) s = s.and((r, q, c) -> r.get("id").in(terms.memberIds(org, term)));
    return members.findAll(s, p).map(BoardMemberDtos.Summary::from);
  }

  @Transactional
  public BoardMemberDtos.TermResponse addTerm(UUID org, UUID member, BoardMemberDtos.CreateTerm r) {
    BoardMember m = require(org, member);
    return BoardMemberDtos.TermResponse.from(
        terms.save(new BoardTerm(m, r.termStart(), r.termEnd())));
  }

  @Transactional
  public BoardMemberDtos.TermResponse endTerm(UUID org, UUID id, BoardTermStatus status) {
    BoardTerm t =
        terms
            .findByIdAndOrganizationId(id, org)
            .orElseThrow(() -> new ResourceNotFoundException("Board term", id));
    t.end(status);
    return BoardMemberDtos.TermResponse.from(t);
  }

  @Transactional(readOnly = true)
  public List<BoardMemberDtos.TermResponse> terms(UUID org, UUID member) {
    require(org, member);
    return terms.findAllByOrganizationIdAndBoardMemberIdOrderByTermStartDesc(org, member).stream()
        .map(BoardMemberDtos.TermResponse::from)
        .toList();
  }

  @Transactional
  public BoardMemberDtos.OfficerResponse addOfficer(
      UUID org, UUID member, BoardMemberDtos.CreateOfficer r) {
    return BoardMemberDtos.OfficerResponse.from(
        officers.save(
            new BoardOfficerAssignment(
                require(org, member),
                r.officerRole(),
                r.otherTitle(),
                r.startDate(),
                r.endDate())));
  }

  @Transactional
  public BoardMemberDtos.OfficerResponse endOfficer(UUID org, UUID id, LocalDate end) {
    BoardOfficerAssignment a =
        officers
            .findByIdAndOrganizationId(id, org)
            .orElseThrow(() -> new ResourceNotFoundException("Board officer assignment", id));
    a.end(end);
    return BoardMemberDtos.OfficerResponse.from(a);
  }

  @Transactional(readOnly = true)
  public List<BoardMemberDtos.OfficerResponse> officers(UUID org, UUID member) {
    require(org, member);
    return officers.forMember(org, member).stream()
        .map(BoardMemberDtos.OfficerResponse::from)
        .toList();
  }
}
