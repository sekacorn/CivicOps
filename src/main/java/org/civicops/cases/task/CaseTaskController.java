package org.civicops.cases.task;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.civicops.cases.casefile.*;
import org.civicops.cases.security.CaseAccessService;
import org.civicops.cases.task.dto.*;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/cases/{caseId}/tasks")
@Tag(name = "Case Tasks")
public class CaseTaskController {
  private final CaseTaskService tasks;
  private final CaseRecordService cases;
  private final CaseAccessService access;

  public CaseTaskController(CaseTaskService t, CaseRecordService c, CaseAccessService a) {
    tasks = t;
    cases = c;
    access = a;
  }

  private void authorize(UUID org, UUID id) {
    access.requireAssignedCaseAccess(org, cases.require(org, id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CaseTaskResponse create(
      @PathVariable UUID organizationId,
      @PathVariable UUID caseId,
      @Valid @RequestBody CreateCaseTaskRequest r) {
    authorize(organizationId, caseId);
    return tasks.create(organizationId, caseId, access.userId(), r);
  }

  @GetMapping
  public Page<CaseTaskResponse> list(
      @PathVariable UUID organizationId,
      @PathVariable UUID caseId,
      @RequestParam(required = false) CaseTaskStatus status,
      @RequestParam(required = false) UUID assignedUserId,
      @RequestParam(required = false) LocalDate dueFrom,
      @RequestParam(required = false) LocalDate dueTo,
      @RequestParam(required = false) Boolean overdue,
      @PageableDefault(size = 20, sort = "dueDate") Pageable p) {
    authorize(organizationId, caseId);
    return tasks.list(
        organizationId,
        caseId,
        status,
        assignedUserId,
        dueFrom,
        dueTo,
        overdue,
        SafePageables.allow(p, Set.of("dueDate", "priority", "createdAt")));
  }

  @PatchMapping("/{taskId}")
  public CaseTaskResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID caseId,
      @PathVariable UUID taskId,
      @Valid @RequestBody UpdateCaseTaskRequest r) {
    authorize(organizationId, caseId);
    return tasks.update(organizationId, caseId, taskId, r);
  }

  @PostMapping("/{taskId}/complete")
  public CaseTaskResponse complete(
      @PathVariable UUID organizationId, @PathVariable UUID caseId, @PathVariable UUID taskId) {
    authorize(organizationId, caseId);
    return tasks.complete(organizationId, caseId, taskId);
  }

  @PostMapping("/{taskId}/cancel")
  public CaseTaskResponse cancel(
      @PathVariable UUID organizationId, @PathVariable UUID caseId, @PathVariable UUID taskId) {
    authorize(organizationId, caseId);
    return tasks.cancel(organizationId, caseId, taskId);
  }
}
