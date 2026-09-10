package org.civicops.grantreporting.template;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.civicops.grantreporting.security.GrantReportingAccessService;
import org.civicops.shared.web.*;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
@Tag(name = "Grant Reporting Templates")
public class GrantReportTemplateController {
  private final GrantReportTemplateService templates;
  private final GrantReportingAccessService access;

  public GrantReportTemplateController(
      GrantReportTemplateService t, GrantReportingAccessService a) {
    templates = t;
    access = a;
  }

  @PostMapping("/grant-report-templates")
  @ResponseStatus(HttpStatus.CREATED)
  public GrantReportTemplateDtos.Response create(
      @PathVariable UUID organizationId, @Valid @RequestBody GrantReportTemplateDtos.Create r) {
    access.requireTemplateManagement(organizationId);
    return templates.create(organizationId, access.userId(), r);
  }

  @GetMapping("/grant-report-templates")
  public Page<GrantReportTemplateDtos.Response> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) Boolean active,
      @RequestParam(required = false) UUID grantId,
      @PageableDefault(size = 20, sort = "name") Pageable p) {
    access.requireReportRead(organizationId);
    return templates.list(
        organizationId, active, grantId, SafePageables.allow(p, Set.of("name", "createdAt")));
  }

  @GetMapping("/grant-report-templates/{id}")
  public GrantReportTemplateDtos.Response detail(
      @PathVariable UUID organizationId, @PathVariable UUID id) {
    access.requireReportRead(organizationId);
    return GrantReportTemplateDtos.Response.from(templates.require(organizationId, id));
  }

  @PatchMapping("/grant-report-templates/{id}")
  public GrantReportTemplateDtos.Response update(
      @PathVariable UUID organizationId,
      @PathVariable UUID id,
      @Valid @RequestBody GrantReportTemplateDtos.Update r) {
    access.requireTemplateManagement(organizationId);
    return templates.update(organizationId, id, r);
  }

  @PostMapping("/grant-report-templates/{id}/sections")
  @ResponseStatus(HttpStatus.CREATED)
  public GrantReportTemplateDtos.SectionResponse section(
      @PathVariable UUID organizationId,
      @PathVariable UUID id,
      @Valid @RequestBody GrantReportTemplateDtos.AddSection r) {
    access.requireTemplateManagement(organizationId);
    return templates.addSection(organizationId, id, r);
  }

  @GetMapping("/grant-report-templates/{id}/sections")
  public List<GrantReportTemplateDtos.SectionResponse> sections(
      @PathVariable UUID organizationId, @PathVariable UUID id) {
    access.requireReportRead(organizationId);
    return templates.sections(organizationId, id);
  }

  @PatchMapping("/grant-report-template-sections/{id}")
  public GrantReportTemplateDtos.SectionResponse updateSection(
      @PathVariable UUID organizationId,
      @PathVariable UUID id,
      @Valid @RequestBody GrantReportTemplateDtos.UpdateSection r) {
    access.requireTemplateManagement(organizationId);
    return templates.updateSection(organizationId, id, r);
  }
}
