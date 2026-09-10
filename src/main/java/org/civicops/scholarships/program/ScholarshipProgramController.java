package org.civicops.scholarships.program;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.civicops.scholarships.program.dto.*;
import org.civicops.scholarships.security.ScholarshipAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/scholarship-programs")
@Tag(name = "Scholarship Programs")
public class ScholarshipProgramController {
  private final ScholarshipProgramService programs;
  private final ScholarshipAccessService access;

  public ScholarshipProgramController(ScholarshipProgramService p, ScholarshipAccessService a) {
    programs = p;
    access = a;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ScholarshipProgramResponse create(
      @PathVariable UUID organizationId, @Valid @RequestBody CreateScholarshipProgramRequest r) {
    access.requireScholarshipManagement(organizationId);
    return programs.create(organizationId, access.userId(), r);
  }

  @GetMapping
  public Page<ScholarshipProgramResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) ScholarshipProgramStatus status,
      @RequestParam(required = false) String academicYear,
      @RequestParam(required = false) LocalDate openFrom,
      @RequestParam(required = false) LocalDate openTo,
      @PageableDefault(size = 20, sort = "applicationDeadline") Pageable p) {
    access.requireProgramRead(organizationId);
    return programs.list(
        organizationId,
        status,
        academicYear,
        openFrom,
        openTo,
        SafePageables.allow(p, Set.of("name", "applicationDeadline", "createdAt")));
  }

  @GetMapping("/{programId}")
  public ScholarshipProgramResponse detail(
      @PathVariable UUID organizationId, @PathVariable UUID programId) {
    access.requireProgramRead(organizationId);
    return programs.detail(organizationId, programId);
  }

  @PatchMapping("/{programId}")
  public ScholarshipProgramResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID programId,
      @Valid @RequestBody UpdateScholarshipProgramRequest r) {
    access.requireScholarshipManagement(organizationId);
    return programs.update(organizationId, programId, r);
  }

  @PostMapping("/{programId}/open")
  public ScholarshipProgramResponse open(
      @PathVariable UUID organizationId, @PathVariable UUID programId) {
    return transition(organizationId, programId, ScholarshipProgramStatus.OPEN);
  }

  @PostMapping("/{programId}/close")
  public ScholarshipProgramResponse close(
      @PathVariable UUID organizationId, @PathVariable UUID programId) {
    return transition(organizationId, programId, ScholarshipProgramStatus.CLOSED);
  }

  @PostMapping("/{programId}/start-review")
  public ScholarshipProgramResponse review(
      @PathVariable UUID organizationId, @PathVariable UUID programId) {
    return transition(organizationId, programId, ScholarshipProgramStatus.REVIEWING);
  }

  @PostMapping("/{programId}/finalize-awards")
  public ScholarshipProgramResponse awarded(
      @PathVariable UUID organizationId, @PathVariable UUID programId) {
    return transition(organizationId, programId, ScholarshipProgramStatus.AWARDED);
  }

  @PostMapping("/{programId}/cancel")
  public ScholarshipProgramResponse cancel(
      @PathVariable UUID organizationId, @PathVariable UUID programId) {
    return transition(organizationId, programId, ScholarshipProgramStatus.CANCELLED);
  }

  private ScholarshipProgramResponse transition(
      UUID org, UUID id, ScholarshipProgramStatus target) {
    access.requireScholarshipManagement(org);
    return programs.transition(org, id, target);
  }
}
