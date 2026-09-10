package org.civicops.board.resolution;

import java.time.*;
import java.util.*;
import org.civicops.board.motion.*;
import org.civicops.core.user.UserService;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardResolutionService {
  private final BoardResolutionRepository resolutions;
  private final BoardMotionService motions;
  private final UserService users;
  private final Clock clock;

  public BoardResolutionService(
      BoardResolutionRepository r, BoardMotionService m, UserService u, Clock c) {
    resolutions = r;
    motions = m;
    users = u;
    clock = c;
  }

  @Transactional
  public BoardResolutionDtos.Response create(
      UUID org, UUID motionId, UUID actor, BoardResolutionDtos.Create r) {
    BoardMotion m = motions.require(org, motionId);
    if (m.getStatus() != MotionStatus.PASSED)
      throw new BusinessRuleException(
          "MOTION_NOT_PASSED", "Only passed motions can create adopted resolutions");
    if (resolutions.existsByOrganizationIdAndResolutionNumberIgnoreCase(
        org, r.resolutionNumber().trim()))
      throw new ConflictException(
          "DUPLICATE_RESOLUTION_NUMBER", "Resolution number already exists");
    return BoardResolutionDtos.Response.from(
        resolutions.save(
            new BoardResolution(
                m,
                r.resolutionNumber(),
                r.title(),
                r.text(),
                r.adoptedDate(),
                users.requireEntity(actor))));
  }

  @Transactional
  public BoardResolutionDtos.Response rescind(UUID org, UUID id) {
    BoardResolution r = require(org, id);
    r.rescind(Instant.now(clock));
    return BoardResolutionDtos.Response.from(r);
  }

  @Transactional(readOnly = true)
  public BoardResolution require(UUID org, UUID id) {
    return resolutions
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Board resolution", id));
  }

  @Transactional(readOnly = true)
  public BoardResolutionDtos.Response detail(UUID org, UUID id) {
    return BoardResolutionDtos.Response.from(require(org, id));
  }

  @Transactional(readOnly = true)
  public Page<BoardResolutionDtos.Response> list(
      UUID org, ResolutionStatus status, LocalDate from, LocalDate to, Pageable p) {
    if (from != null && to != null && to.isBefore(from))
      throw new BusinessRuleException("INVALID_DATE_RANGE", "Resolution filter end precedes start");
    Specification<BoardResolution> s = (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (status != null) s = s.and((r, q, c) -> c.equal(r.get("status"), status));
    if (from != null) s = s.and((r, q, c) -> c.greaterThanOrEqualTo(r.get("adoptedDate"), from));
    if (to != null) s = s.and((r, q, c) -> c.lessThanOrEqualTo(r.get("adoptedDate"), to));
    return resolutions.findAll(s, p).map(BoardResolutionDtos.Response::from);
  }
}
