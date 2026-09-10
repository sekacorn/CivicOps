package org.civicops.cases.note;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.civicops.cases.casefile.*;
import org.civicops.cases.note.dto.*;
import org.civicops.cases.security.CaseAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/cases/{caseId}/notes")
@Tag(name = "Case Notes")
public class CaseNoteController {
  private final CaseNoteService notes;
  private final CaseRecordService cases;
  private final CaseAccessService access;

  public CaseNoteController(CaseNoteService n, CaseRecordService c, CaseAccessService a) {
    notes = n;
    cases = c;
    access = a;
  }

  private void authorize(UUID org, UUID id) {
    access.requireAssignedCaseAccess(org, cases.require(org, id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CaseNoteResponse create(
      @PathVariable UUID organizationId,
      @PathVariable UUID caseId,
      @Valid @RequestBody CreateCaseNoteRequest r) {
    authorize(organizationId, caseId);
    return notes.create(organizationId, caseId, access.userId(), r);
  }

  @GetMapping
  public Page<CaseNoteResponse> list(
      @PathVariable UUID organizationId,
      @PathVariable UUID caseId,
      @PageableDefault(size = 20, sort = "createdAt") Pageable p) {
    authorize(organizationId, caseId);
    return notes.list(organizationId, caseId, SafePageables.allow(p, Set.of("createdAt")));
  }

  @GetMapping("/{noteId}")
  public CaseNoteResponse detail(
      @PathVariable UUID organizationId, @PathVariable UUID caseId, @PathVariable UUID noteId) {
    authorize(organizationId, caseId);
    return notes.detail(organizationId, caseId, noteId);
  }
}
