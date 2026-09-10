package org.civicops.cases.task;

import java.time.*;
import java.util.*;
import org.civicops.cases.casefile.*;
import org.civicops.cases.task.dto.*;
import org.civicops.core.membership.OrganizationMembershipRepository;
import org.civicops.core.user.*;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaseTaskService {
  private final CaseTaskRepository tasks;
  private final CaseRecordService cases;
  private final UserService users;
  private final OrganizationMembershipRepository memberships;
  private final Clock clock;

  public CaseTaskService(
      CaseTaskRepository t,
      CaseRecordService c,
      UserService u,
      OrganizationMembershipRepository m,
      Clock clock) {
    tasks = t;
    cases = c;
    users = u;
    memberships = m;
    this.clock = clock;
  }

  @Transactional
  public CaseTaskResponse create(UUID org, UUID caseId, UUID creator, CreateCaseTaskRequest r) {
    CaseRecord c = active(org, caseId);
    User assignee = member(org, r.assignedUserId());
    CaseTask t =
        new CaseTask(
            c.getOrganization(),
            c,
            users.requireEntity(creator),
            r.title().trim(),
            clean(r.description()),
            assignee,
            r.dueDate(),
            r.priority());
    return response(tasks.save(t));
  }

  @Transactional(readOnly = true)
  public CaseTask require(UUID org, UUID caseId, UUID id) {
    return tasks
        .findByIdAndOrganizationIdAndCaseRecordId(id, org, caseId)
        .orElseThrow(() -> new ResourceNotFoundException("Case task", id));
  }

  @Transactional
  public CaseTaskResponse update(UUID org, UUID caseId, UUID id, UpdateCaseTaskRequest r) {
    active(org, caseId);
    CaseTask t = require(org, caseId, id);
    if (r.status() == CaseTaskStatus.COMPLETED || r.status() == CaseTaskStatus.CANCELLED)
      throw new BusinessRuleException(
          "EXPLICIT_TASK_TRANSITION_REQUIRED", "Use the task lifecycle endpoint");
    t.update(
        clean(r.title()),
        clean(r.description()),
        member(org, r.assignedUserId()),
        r.dueDate(),
        r.priority(),
        r.status());
    return response(t);
  }

  @Transactional
  public CaseTaskResponse complete(UUID org, UUID caseId, UUID id) {
    active(org, caseId);
    CaseTask t = require(org, caseId, id);
    t.complete(Instant.now(clock));
    return response(t);
  }

  @Transactional
  public CaseTaskResponse cancel(UUID org, UUID caseId, UUID id) {
    active(org, caseId);
    CaseTask t = require(org, caseId, id);
    t.cancel();
    return response(t);
  }

  @Transactional(readOnly = true)
  public Page<CaseTaskResponse> list(
      UUID org,
      UUID caseId,
      CaseTaskStatus status,
      UUID assigned,
      LocalDate from,
      LocalDate to,
      Boolean overdue,
      Pageable p) {
    if (from != null && to != null && to.isBefore(from))
      throw new BusinessRuleException(
          "INVALID_DATE_RANGE", "Due-date filter end must not precede start");
    cases.require(org, caseId);
    Specification<CaseTask> s =
        (r, q, c) ->
            c.and(
                c.equal(r.get("organization").get("id"), org),
                c.equal(r.get("caseRecord").get("id"), caseId));
    if (status != null) s = s.and((r, q, c) -> c.equal(r.get("status"), status));
    if (assigned != null)
      s = s.and((r, q, c) -> c.equal(r.get("assignedUser").get("id"), assigned));
    if (from != null) s = s.and((r, q, c) -> c.greaterThanOrEqualTo(r.get("dueDate"), from));
    if (to != null) s = s.and((r, q, c) -> c.lessThanOrEqualTo(r.get("dueDate"), to));
    if (Boolean.TRUE.equals(overdue)) {
      LocalDate today = LocalDate.now(clock);
      s =
          s.and(
              (r, q, c) ->
                  c.and(
                      c.lessThan(r.get("dueDate"), today),
                      r.get("status").in(CaseTaskStatus.OPEN, CaseTaskStatus.IN_PROGRESS)));
    }
    return tasks.findAll(s, p).map(this::response);
  }

  private CaseRecord active(UUID org, UUID id) {
    CaseRecord c = cases.require(org, id);
    if (c.isTerminal())
      throw new BusinessRuleException(
          "TERMINAL_CASE_HISTORY_ONLY",
          "Closed or cancelled cases preserve history but cannot accept task changes");
    return c;
  }

  private User member(UUID org, UUID id) {
    if (id == null) return null;
    User u = users.requireEntity(id);
    if (!u.isActive() || memberships.findByOrganizationIdAndUserIdAndActiveTrue(org, id).isEmpty())
      throw new BusinessRuleException(
          "INVALID_TASK_ASSIGNEE", "Task assignee must be an active organization member");
    return u;
  }

  private CaseTaskResponse response(CaseTask t) {
    return CaseTaskResponse.from(t, LocalDate.now(clock));
  }

  private static String clean(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
