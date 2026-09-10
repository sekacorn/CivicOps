package org.civicops.cases.client;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.civicops.cases.casefile.*;
import org.civicops.cases.casefile.dto.*;
import org.civicops.cases.client.dto.*;
import org.civicops.cases.security.CaseAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/clients")
@Tag(name = "Case Management Clients")
public class ClientController {
  private final ClientService clients;
  private final CaseRecordService cases;
  private final CaseAccessService access;

  public ClientController(ClientService c, CaseRecordService cases, CaseAccessService a) {
    clients = c;
    this.cases = cases;
    access = a;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ClientDetailResponse create(
      @PathVariable UUID organizationId, @Valid @RequestBody CreateClientRequest r) {
    access.requireCaseManager(organizationId);
    return clients.create(organizationId, r);
  }

  @GetMapping
  public Page<ClientSummaryResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) Boolean active,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String externalReferenceNumber,
      @RequestParam(required = false) String name,
      @PageableDefault(size = 20, sort = "lastName") Pageable p) {
    access.requireCaseManager(organizationId);
    return clients.list(
        organizationId,
        active,
        email,
        externalReferenceNumber,
        name,
        SafePageables.allow(p, Set.of("lastName", "createdAt")));
  }

  @GetMapping("/{clientId}")
  public ClientDetailResponse detail(
      @PathVariable UUID organizationId, @PathVariable UUID clientId) {
    access.requireClientDetails(organizationId, clientId);
    return clients.detail(organizationId, clientId);
  }

  @PatchMapping("/{clientId}")
  public ClientDetailResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID clientId,
      @Valid @RequestBody UpdateClientRequest r) {
    access.requireCaseManager(organizationId);
    return clients.update(organizationId, clientId, r);
  }

  @GetMapping("/{clientId}/cases")
  public Page<CaseSummaryResponse> history(
      @PathVariable UUID organizationId,
      @PathVariable UUID clientId,
      @PageableDefault(size = 20, sort = "openedDate") Pageable p) {
    access.requireClientDetails(organizationId, clientId);
    UUID scope = access.requireListAccessAndWorkerScope(organizationId);
    return cases.list(
        organizationId,
        null,
        null,
        null,
        clientId,
        null,
        null,
        null,
        scope,
        SafePageables.allow(
            p, Set.of("openedDate", "priority", "status", "updatedAt", "caseNumber")));
  }
}
