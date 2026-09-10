package org.civicops.cases.casefile;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.civicops.cases.casefile.dto.*;
import org.civicops.cases.security.CaseAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/cases")
@Tag(name = "Case Management Cases")
public class CaseRecordController {
  private final CaseRecordService cases;
  private final CaseAccessService access;

  public CaseRecordController(CaseRecordService c, CaseAccessService a) {
    cases = c;
    access = a;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CaseDetailResponse create(
      @PathVariable UUID organizationId, @Valid @RequestBody CreateCaseRequest r) {
    access.requireCaseManager(organizationId);
    return cases.create(organizationId, access.userId(), r);
  }

  @GetMapping
  public Page<CaseSummaryResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) CaseStatus status,
      @RequestParam(required = false) CasePriority priority,
      @RequestParam(required = false) CaseType caseType,
      @RequestParam(required = false) UUID clientId,
      @RequestParam(required = false) UUID assignedUserId,
      @RequestParam(required = false) LocalDate openedFrom,
      @RequestParam(required = false) LocalDate openedTo,
      @PageableDefault(size = 20, sort = "openedDate") Pageable p) {
    UUID scope = access.requireListAccessAndWorkerScope(organizationId);
    return cases.list(
        organizationId,
        status,
        priority,
        caseType,
        clientId,
        assignedUserId,
        openedFrom,
        openedTo,
        scope,
        SafePageables.allow(
            p, Set.of("openedDate", "priority", "status", "updatedAt", "caseNumber")));
  }

  @GetMapping("/{caseId}")
  public CaseDetailResponse detail(@PathVariable UUID organizationId, @PathVariable UUID caseId) {
    CaseRecord c = cases.require(organizationId, caseId);
    access.requireSensitiveCaseAccess(organizationId, c);
    return cases.detail(organizationId, caseId);
  }

  @PatchMapping("/{caseId}")
  public CaseDetailResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID caseId,
      @Valid @RequestBody UpdateCaseRequest r) {
    access.requireCaseManager(organizationId);
    return cases.update(organizationId, caseId, r);
  }

  @PostMapping("/{caseId}/assign")
  public CaseDetailResponse assign(
      @PathVariable UUID organizationId,
      @PathVariable UUID caseId,
      @Valid @RequestBody AssignCaseRequest r) {
    access.requireCaseManager(organizationId);
    return cases.assign(organizationId, caseId, r.userId());
  }

  @PostMapping("/{caseId}/start")
  public CaseDetailResponse start(@PathVariable UUID organizationId, @PathVariable UUID caseId) {
    return transition(organizationId, caseId, CaseStatus.IN_PROGRESS);
  }

  @PostMapping("/{caseId}/hold")
  public CaseDetailResponse hold(@PathVariable UUID organizationId, @PathVariable UUID caseId) {
    return transition(organizationId, caseId, CaseStatus.ON_HOLD);
  }

  @PostMapping("/{caseId}/resume")
  public CaseDetailResponse resume(@PathVariable UUID organizationId, @PathVariable UUID caseId) {
    return transition(organizationId, caseId, CaseStatus.IN_PROGRESS);
  }

  @PostMapping("/{caseId}/close")
  public CaseDetailResponse close(@PathVariable UUID organizationId, @PathVariable UUID caseId) {
    return transition(organizationId, caseId, CaseStatus.CLOSED);
  }

  @PostMapping("/{caseId}/cancel")
  public CaseDetailResponse cancel(@PathVariable UUID organizationId, @PathVariable UUID caseId) {
    return transition(organizationId, caseId, CaseStatus.CANCELLED);
  }

  private CaseDetailResponse transition(UUID org, UUID id, CaseStatus target) {
    access.requireCaseManager(org);
    return cases.transition(org, id, target);
  }
}
