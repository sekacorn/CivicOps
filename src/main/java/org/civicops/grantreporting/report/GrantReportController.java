package org.civicops.grantreporting.report;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.civicops.grantreporting.evidence.*;
import org.civicops.grantreporting.export.GrantReportExportService;
import org.civicops.grantreporting.security.GrantReportingAccessService;
import org.civicops.shared.exception.BusinessRuleException;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
@Tag(name = "Grant Reporting Assistant")
public class GrantReportController {
  private final GrantReportService reports;
  private final GrantReportExportService exports;
  private final GrantReportingAccessService access;

  public GrantReportController(
      GrantReportService r, GrantReportExportService e, GrantReportingAccessService a) {
    reports = r;
    exports = e;
    access = a;
  }

  @PostMapping("/grants/{grantId}/reports")
  @ResponseStatus(HttpStatus.CREATED)
  public GrantReportDtos.Detail create(
      @PathVariable UUID organizationId,
      @PathVariable UUID grantId,
      @Valid @RequestBody GrantReportDtos.Create r) {
    access.requireReportManagement(organizationId);
    return reports.create(organizationId, grantId, access.userId(), r);
  }

  @GetMapping("/grant-reports")
  public Page<GrantReportDtos.Summary> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) UUID grantId,
      @RequestParam(required = false) GrantReportStatus status,
      @PageableDefault(size = 20, sort = "createdAt") Pageable p) {
    access.requireReportRead(organizationId);
    boolean manager = access.canManage(organizationId);
    return reports.list(
        organizationId,
        grantId,
        status,
        manager,
        SafePageables.allow(
            p, Set.of("reportingPeriodStart", "reportingPeriodEnd", "status", "createdAt")));
  }

  @GetMapping("/grant-reports/{id}")
  public GrantReportDtos.Detail detail(@PathVariable UUID organizationId, @PathVariable UUID id) {
    access.requireReportRead(organizationId);
    return reports.detail(organizationId, id, access.canManage(organizationId));
  }

  @PatchMapping("/grant-reports/{id}")
  public GrantReportDtos.Detail update(
      @PathVariable UUID organizationId,
      @PathVariable UUID id,
      @Valid @RequestBody GrantReportDtos.Update r) {
    access.requireReportManagement(organizationId);
    return reports.update(organizationId, id, r);
  }

  @PostMapping("/grant-reports/{id}/generate")
  public GrantReportDtos.Detail generate(@PathVariable UUID organizationId, @PathVariable UUID id) {
    access.requireReportManagement(organizationId);
    return reports.generate(organizationId, id, false);
  }

  @PostMapping("/grant-reports/{id}/regenerate")
  public GrantReportDtos.Detail regenerate(
      @PathVariable UUID organizationId, @PathVariable UUID id) {
    access.requireReportManagement(organizationId);
    return reports.generate(organizationId, id, true);
  }

  @PostMapping("/grant-reports/{id}/finalize")
  public GrantReportDtos.Detail finalizeReport(
      @PathVariable UUID organizationId, @PathVariable UUID id) {
    access.requireFinalize(organizationId);
    return reports.finalizeReport(organizationId, id, access.userId());
  }

  @GetMapping("/grant-reports/{id}/sections")
  public List<GrantReportDtos.SectionResponse> sections(
      @PathVariable UUID organizationId, @PathVariable UUID id) {
    access.requireReportRead(organizationId);
    return reports.sectionList(organizationId, id, access.canManage(organizationId));
  }

  @GetMapping("/grant-report-sections/{id}")
  public GrantReportDtos.SectionResponse section(
      @PathVariable UUID organizationId, @PathVariable UUID id) {
    access.requireReportRead(organizationId);
    var s = reports.requireSection(organizationId, id);
    if (!access.canManage(organizationId)
        && s.getReport().getStatus() != GrantReportStatus.FINALIZED)
      throw new org.springframework.security.access.AccessDeniedException(
          "Only finalized sections are readable");
    return GrantReportDtos.SectionResponse.from(s);
  }

  @PatchMapping("/grant-report-sections/{id}")
  public GrantReportDtos.SectionResponse edit(
      @PathVariable UUID organizationId,
      @PathVariable UUID id,
      @Valid @RequestBody GrantReportDtos.EditSection r) {
    access.requireReportManagement(organizationId);
    return reports.editSection(organizationId, id, r.editedContent());
  }

  @PostMapping("/grant-report-sections/{id}/approve")
  public GrantReportDtos.SectionResponse approve(
      @PathVariable UUID organizationId, @PathVariable UUID id) {
    access.requireReportManagement(organizationId);
    return reports.approveSection(organizationId, id);
  }

  @GetMapping("/grant-reports/{id}/evidence")
  public List<EvidenceDtos.Response> evidence(
      @PathVariable UUID organizationId, @PathVariable UUID id) {
    access.requireEvidenceRead(organizationId);
    return reports.evidence(organizationId, id, access.canManage(organizationId));
  }

  @PostMapping("/grant-reports/{id}/manual-evidence")
  @ResponseStatus(HttpStatus.CREATED)
  public EvidenceDtos.Response manual(
      @PathVariable UUID organizationId,
      @PathVariable UUID id,
      @Valid @RequestBody EvidenceDtos.ManualEvidence r) {
    access.requireReportManagement(organizationId);
    return reports.addManual(organizationId, id, access.userId(), r);
  }

  @GetMapping("/grant-reports/{id}/missing-data")
  public List<EvidenceDtos.Missing> missing(
      @PathVariable UUID organizationId, @PathVariable UUID id) {
    access.requireEvidenceRead(organizationId);
    return reports.missing(organizationId, id, access.canManage(organizationId));
  }

  @GetMapping("/grant-reports/{id}/export")
  public ResponseEntity<?> export(
      @PathVariable UUID organizationId,
      @PathVariable UUID id,
      @RequestParam(defaultValue = "json") String format) {
    access.requireReportRead(organizationId);
    boolean manager = access.canManage(organizationId);
    if (format.equalsIgnoreCase("json"))
      return ResponseEntity.ok(exports.json(organizationId, id, manager));
    if (format.equalsIgnoreCase("markdown"))
      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType("text/markdown"))
          .body(exports.markdown(organizationId, id, manager));
    throw new BusinessRuleException(
        "UNSUPPORTED_EXPORT_FORMAT", "Supported formats are json and markdown");
  }
}
