package org.civicops.grants.grant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.civicops.grants.grant.dto.*;
import org.civicops.grants.security.GrantAccessService;
import org.civicops.grants.support.GrantPageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/grants")
@Tag(name = "Grants", description = "Organization-scoped grant management and lifecycle")
public class GrantController {
  private static final Set<String> SORTS =
      Set.of("grantName", "startDate", "endDate", "reportingDeadline", "createdAt");
  private final GrantService service;
  private final GrantAccessService access;

  public GrantController(GrantService service, GrantAccessService access) {
    this.service = service;
    this.access = access;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a prospect grant")
  public GrantDetailResponse create(
      @PathVariable UUID organizationId, @Valid @RequestBody CreateGrantRequest request) {
    access.requireManage(organizationId);
    return service.create(organizationId, access.userId(), request);
  }

  @GetMapping
  @Operation(
      summary = "List grants",
      description =
          "Filters: status, grantor, reportingDeadlineFrom, "
              + "reportingDeadlineTo, startFrom, endBefore, restricted. Sort: grantName, startDate, endDate, "
              + "reportingDeadline, createdAt. Maximum page size: 100.")
  public Page<GrantSummaryResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) GrantStatus status,
      @RequestParam(required = false) String grantor,
      @RequestParam(required = false) LocalDate reportingDeadlineFrom,
      @RequestParam(required = false) LocalDate reportingDeadlineTo,
      @RequestParam(required = false) LocalDate startFrom,
      @RequestParam(required = false) LocalDate endBefore,
      @RequestParam(required = false) Boolean restricted,
      @PageableDefault(size = 20, sort = "grantName") Pageable pageable) {
    access.requireRead(organizationId);
    return service.list(
        organizationId,
        status,
        grantor,
        reportingDeadlineFrom,
        reportingDeadlineTo,
        startFrom,
        endBefore,
        restricted,
        GrantPageables.allow(pageable, SORTS));
  }

  @GetMapping("/{grantId}")
  public GrantDetailResponse detail(@PathVariable UUID organizationId, @PathVariable UUID grantId) {
    access.requireManage(organizationId);
    return service.detail(organizationId, grantId);
  }

  @PatchMapping("/{grantId}")
  @Operation(
      summary = "Safely update non-status grant metadata",
      description =
          "Organization, status, creator and audit fields cannot be changed. JSON null follows the CivicOps omitted-field convention.")
  public GrantDetailResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID grantId,
      @Valid @RequestBody UpdateGrantRequest request) {
    access.requireManage(organizationId);
    return service.update(organizationId, grantId, request);
  }

  @PostMapping("/{grantId}/start-application")
  public GrantDetailResponse start(@PathVariable UUID organizationId, @PathVariable UUID grantId) {
    return transition(organizationId, grantId, GrantStatus.APPLICATION_IN_PROGRESS);
  }

  @PostMapping("/{grantId}/submit")
  public GrantDetailResponse submit(@PathVariable UUID organizationId, @PathVariable UUID grantId) {
    return transition(organizationId, grantId, GrantStatus.SUBMITTED);
  }

  @PostMapping("/{grantId}/mark-awarded")
  public GrantDetailResponse award(@PathVariable UUID organizationId, @PathVariable UUID grantId) {
    return transition(organizationId, grantId, GrantStatus.AWARDED);
  }

  @PostMapping("/{grantId}/activate")
  public GrantDetailResponse activate(
      @PathVariable UUID organizationId, @PathVariable UUID grantId) {
    return transition(organizationId, grantId, GrantStatus.ACTIVE);
  }

  @PostMapping("/{grantId}/close")
  public GrantDetailResponse close(@PathVariable UUID organizationId, @PathVariable UUID grantId) {
    return transition(organizationId, grantId, GrantStatus.CLOSED);
  }

  @PostMapping("/{grantId}/reject")
  public GrantDetailResponse reject(@PathVariable UUID organizationId, @PathVariable UUID grantId) {
    return transition(organizationId, grantId, GrantStatus.REJECTED);
  }

  @PostMapping("/{grantId}/withdraw")
  public GrantDetailResponse withdraw(
      @PathVariable UUID organizationId, @PathVariable UUID grantId) {
    return transition(organizationId, grantId, GrantStatus.WITHDRAWN);
  }

  private GrantDetailResponse transition(UUID organizationId, UUID grantId, GrantStatus target) {
    access.requireManage(organizationId);
    return service.transition(organizationId, grantId, target);
  }
}
