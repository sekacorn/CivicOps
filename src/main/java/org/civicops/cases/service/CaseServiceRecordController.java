package org.civicops.cases.service;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.civicops.cases.casefile.*;
import org.civicops.cases.security.CaseAccessService;
import org.civicops.cases.service.dto.*;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/cases/{caseId}/services")
@Tag(name = "Case Service Records")
public class CaseServiceRecordController {
  private final CaseServiceRecordService services;
  private final CaseRecordService cases;
  private final CaseAccessService access;

  public CaseServiceRecordController(
      CaseServiceRecordService s, CaseRecordService c, CaseAccessService a) {
    services = s;
    cases = c;
    access = a;
  }

  private void authorize(UUID org, UUID id) {
    access.requireAssignedCaseAccess(org, cases.require(org, id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CaseServiceResponse create(
      @PathVariable UUID organizationId,
      @PathVariable UUID caseId,
      @Valid @RequestBody CreateCaseServiceRequest r) {
    authorize(organizationId, caseId);
    return services.create(organizationId, caseId, r);
  }

  @GetMapping
  public Page<CaseServiceResponse> list(
      @PathVariable UUID organizationId,
      @PathVariable UUID caseId,
      @RequestParam(required = false) CaseServiceType serviceType,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @PageableDefault(size = 20, sort = "serviceDate") Pageable p) {
    authorize(organizationId, caseId);
    return services.list(
        organizationId,
        caseId,
        serviceType,
        from,
        to,
        SafePageables.allow(p, Set.of("serviceDate", "createdAt")));
  }

  @GetMapping("/{serviceRecordId}")
  public CaseServiceResponse detail(
      @PathVariable UUID organizationId,
      @PathVariable UUID caseId,
      @PathVariable UUID serviceRecordId) {
    authorize(organizationId, caseId);
    return services.detail(organizationId, caseId, serviceRecordId);
  }
}
