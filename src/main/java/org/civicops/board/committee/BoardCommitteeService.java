package org.civicops.board.committee;

import java.time.LocalDate;
import java.util.*;
import org.civicops.board.member.*;
import org.civicops.core.organization.OrganizationService;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardCommitteeService {
  private final BoardCommitteeRepository committees;
  private final BoardCommitteeMembershipRepository memberships;
  private final BoardMemberService members;
  private final OrganizationService organizations;

  public BoardCommitteeService(
      BoardCommitteeRepository c,
      BoardCommitteeMembershipRepository m,
      BoardMemberService bm,
      OrganizationService o) {
    committees = c;
    memberships = m;
    members = bm;
    organizations = o;
  }

  @Transactional
  public BoardCommitteeDtos.Response create(UUID org, BoardCommitteeDtos.Create r) {
    if (committees.existsByOrganizationIdAndNameIgnoreCase(org, r.name().trim()))
      throw new ConflictException("DUPLICATE_BOARD_COMMITTEE", "Committee name already exists");
    return BoardCommitteeDtos.Response.from(
        committees.save(
            new BoardCommittee(organizations.requireEntity(org), r.name(), r.description())));
  }

  @Transactional
  public BoardCommitteeDtos.Response update(UUID org, UUID id, BoardCommitteeDtos.Update r) {
    BoardCommittee c = require(org, id);
    if (r.name() != null
        && !r.name().equalsIgnoreCase(c.getName())
        && committees.existsByOrganizationIdAndNameIgnoreCase(org, r.name().trim()))
      throw new ConflictException("DUPLICATE_BOARD_COMMITTEE", "Committee name already exists");
    c.update(r.name(), r.description());
    return BoardCommitteeDtos.Response.from(c);
  }

  @Transactional
  public BoardCommitteeDtos.Response deactivate(UUID org, UUID id) {
    BoardCommittee c = require(org, id);
    c.deactivate();
    return BoardCommitteeDtos.Response.from(c);
  }

  @Transactional(readOnly = true)
  public BoardCommittee require(UUID org, UUID id) {
    return committees
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Board committee", id));
  }

  @Transactional(readOnly = true)
  public Page<BoardCommitteeDtos.Response> list(UUID org, Boolean active, Pageable p) {
    Specification<BoardCommittee> s = (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (active != null) s = s.and((r, q, c) -> c.equal(r.get("active"), active));
    return committees.findAll(s, p).map(BoardCommitteeDtos.Response::from);
  }

  @Transactional
  public BoardCommitteeDtos.MembershipResponse addMember(
      UUID org, UUID committee, BoardCommitteeDtos.AddMember r) {
    BoardCommittee c = require(org, committee);
    BoardMember m = members.require(org, r.boardMemberId());
    if (!c.isActive() || !m.isActive())
      throw new BusinessRuleException(
          "INACTIVE_COMMITTEE_MEMBER", "Committee and member must be active");
    if (memberships.existsByCommitteeIdAndBoardMemberIdAndActiveTrue(committee, m.getId()))
      throw new ConflictException(
          "DUPLICATE_COMMITTEE_MEMBERSHIP", "Member already has an active committee assignment");
    return BoardCommitteeDtos.MembershipResponse.from(
        memberships.save(new BoardCommitteeMembership(c, m, r.role(), r.startDate())));
  }

  @Transactional
  public BoardCommitteeDtos.MembershipResponse endMember(UUID org, UUID id, LocalDate end) {
    BoardCommitteeMembership m =
        memberships
            .findByIdAndOrganizationId(id, org)
            .orElseThrow(() -> new ResourceNotFoundException("Board committee membership", id));
    m.end(end);
    return BoardCommitteeDtos.MembershipResponse.from(m);
  }

  @Transactional(readOnly = true)
  public List<BoardCommitteeDtos.MembershipResponse> members(UUID org, UUID committee) {
    require(org, committee);
    return memberships
        .findAllByOrganizationIdAndCommitteeIdOrderByStartDate(org, committee)
        .stream()
        .map(BoardCommitteeDtos.MembershipResponse::from)
        .toList();
  }
}
