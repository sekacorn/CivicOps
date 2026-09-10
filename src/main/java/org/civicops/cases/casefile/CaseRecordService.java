package org.civicops.cases.casefile;

import java.time.*;
import java.util.*;
import org.civicops.cases.casefile.dto.*;
import org.civicops.cases.client.ClientService;
import org.civicops.core.membership.*;
import org.civicops.core.organization.OrganizationService;
import org.civicops.core.user.*;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaseRecordService {
  private final CaseRecordRepository cases;
  private final ClientService clients;
  private final OrganizationService organizations;
  private final UserService users;
  private final OrganizationMembershipRepository memberships;
  private final Clock clock;

  public CaseRecordService(
      CaseRecordRepository r,
      ClientService c,
      OrganizationService o,
      UserService u,
      OrganizationMembershipRepository m,
      Clock clock) {
    cases = r;
    clients = c;
    organizations = o;
    users = u;
    memberships = m;
    this.clock = clock;
  }

  @Transactional
  public CaseDetailResponse create(UUID org, UUID creator, CreateCaseRequest r) {
    if (cases.existsByOrganizationIdAndCaseNumber(org, r.caseNumber().trim()))
      throw new ConflictException(
          "DUPLICATE_CASE_NUMBER", "Case number already exists in this organization");
    LocalDate opened = r.openedDate() == null ? LocalDate.now(clock) : r.openedDate();
    return CaseDetailResponse.from(
        cases.save(
            new CaseRecord(
                organizations.requireEntity(org),
                clients.require(org, r.clientId()),
                users.requireEntity(creator),
                r.caseNumber().trim(),
                r.title().trim(),
                clean(r.description()),
                r.caseType(),
                r.priority(),
                opened,
                clean(r.programName()),
                clean(r.intakeSource()))));
  }

  @Transactional(readOnly = true)
  public CaseRecord require(UUID org, UUID id) {
    return cases
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Case", id));
  }

  @Transactional(readOnly = true)
  public CaseDetailResponse detail(UUID org, UUID id) {
    return CaseDetailResponse.from(require(org, id));
  }

  @Transactional
  public CaseDetailResponse update(UUID org, UUID id, UpdateCaseRequest r) {
    CaseRecord c = require(org, id);
    c.update(
        clean(r.title()),
        clean(r.description()),
        r.caseType(),
        r.priority(),
        clean(r.programName()),
        clean(r.intakeSource()));
    return CaseDetailResponse.from(c);
  }

  @Transactional
  public CaseDetailResponse assign(UUID org, UUID id, UUID userId) {
    CaseRecord c = require(org, id);
    User u = users.requireEntity(userId);
    if (!u.isActive())
      throw new BusinessRuleException("INVALID_CASE_ASSIGNEE", "Assignee must be active");
    OrganizationMembership m =
        memberships
            .findByOrganizationIdAndUserIdAndActiveTrue(org, userId)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "INVALID_CASE_ASSIGNEE", "Assignee must be an active organization member"));
    if (m.getRole() != Role.CASE_WORKER
        && m.getRole() != Role.CASE_MANAGER
        && m.getRole() != Role.ORG_ADMIN)
      throw new BusinessRuleException(
          "INVALID_CASE_ASSIGNEE", "Assignee needs a Case Management role");
    c.assign(u);
    return CaseDetailResponse.from(c);
  }

  @Transactional
  public CaseDetailResponse transition(UUID org, UUID id, CaseStatus target) {
    CaseRecord c = require(org, id);
    c.transition(target, LocalDate.now(clock));
    return CaseDetailResponse.from(c);
  }

  @Transactional(readOnly = true)
  public Page<CaseSummaryResponse> list(
      UUID org,
      CaseStatus status,
      CasePriority priority,
      CaseType type,
      UUID client,
      UUID assigned,
      LocalDate from,
      LocalDate to,
      UUID workerScope,
      Pageable p) {
    if (from != null && to != null && to.isBefore(from))
      throw new BusinessRuleException("INVALID_DATE_RANGE", "Filter end must not precede start");
    Specification<CaseRecord> s = (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (workerScope != null)
      s = s.and((r, q, c) -> c.equal(r.get("assignedUser").get("id"), workerScope));
    if (status != null) s = s.and((r, q, c) -> c.equal(r.get("status"), status));
    if (priority != null) s = s.and((r, q, c) -> c.equal(r.get("priority"), priority));
    if (type != null) s = s.and((r, q, c) -> c.equal(r.get("caseType"), type));
    if (client != null) s = s.and((r, q, c) -> c.equal(r.get("client").get("id"), client));
    if (assigned != null)
      s = s.and((r, q, c) -> c.equal(r.get("assignedUser").get("id"), assigned));
    if (from != null) s = s.and((r, q, c) -> c.greaterThanOrEqualTo(r.get("openedDate"), from));
    if (to != null) s = s.and((r, q, c) -> c.lessThanOrEqualTo(r.get("openedDate"), to));
    return cases.findAll(s, p).map(CaseSummaryResponse::from);
  }

  private static String clean(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
