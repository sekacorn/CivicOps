package org.civicops.grantreporting.template;

import java.util.*;
import org.civicops.core.organization.OrganizationService;
import org.civicops.core.user.UserService;
import org.civicops.grants.grant.GrantService;
import org.civicops.shared.exception.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GrantReportTemplateService {
  private final GrantReportTemplateRepository templates;
  private final GrantReportTemplateSectionRepository sections;
  private final OrganizationService organizations;
  private final UserService users;
  private final GrantService grants;

  public GrantReportTemplateService(
      GrantReportTemplateRepository t,
      GrantReportTemplateSectionRepository s,
      OrganizationService o,
      UserService u,
      GrantService g) {
    templates = t;
    sections = s;
    organizations = o;
    users = u;
    grants = g;
  }

  @Transactional
  public GrantReportTemplateDtos.Response create(
      UUID org, UUID actor, GrantReportTemplateDtos.Create r) {
    var grant = r.grantId() == null ? null : grants.require(org, r.grantId());
    return GrantReportTemplateDtos.Response.from(
        templates.save(
            new GrantReportTemplate(
                organizations.requireEntity(org),
                grant,
                r.name(),
                r.description(),
                users.requireEntity(actor))));
  }

  @Transactional(readOnly = true)
  public GrantReportTemplate require(UUID org, UUID id) {
    return templates
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Grant report template", id));
  }

  @Transactional(readOnly = true)
  public Page<GrantReportTemplateDtos.Response> list(
      UUID org, Boolean active, UUID grant, Pageable p) {
    Specification<GrantReportTemplate> s =
        (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (active != null) s = s.and((r, q, c) -> c.equal(r.get("active"), active));
    if (grant != null) s = s.and((r, q, c) -> c.equal(r.get("grant").get("id"), grant));
    return templates.findAll(s, p).map(GrantReportTemplateDtos.Response::from);
  }

  @Transactional
  public GrantReportTemplateDtos.Response update(
      UUID org, UUID id, GrantReportTemplateDtos.Update r) {
    var t = require(org, id);
    t.update(r.name(), r.description(), r.active());
    return GrantReportTemplateDtos.Response.from(t);
  }

  @Transactional
  public GrantReportTemplateDtos.SectionResponse addSection(
      UUID org, UUID id, GrantReportTemplateDtos.AddSection r) {
    var t = require(org, id);
    if (sections.existsByTemplateIdAndSectionKeyIgnoreCase(id, r.sectionKey()))
      throw new ConflictException(
          "DUPLICATE_TEMPLATE_SECTION_KEY", "Template section key already exists");
    if (sections.existsByTemplateIdAndSequenceNumber(id, r.sequenceNumber()))
      throw new ConflictException(
          "DUPLICATE_TEMPLATE_SECTION_SEQUENCE", "Template section sequence already exists");
    try {
      return GrantReportTemplateDtos.SectionResponse.from(
          sections.saveAndFlush(
              new GrantReportTemplateSection(
                  t,
                  r.sectionKey(),
                  r.title(),
                  r.instructions(),
                  r.sequenceNumber(),
                  r.sectionType(),
                  r.required(),
                  r.maxLength())));
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException(
          "DUPLICATE_TEMPLATE_SECTION", "Template section key or sequence already exists");
    }
  }

  @Transactional(readOnly = true)
  public List<GrantReportTemplateDtos.SectionResponse> sections(UUID org, UUID id) {
    require(org, id);
    return sections.findAllByOrganizationIdAndTemplateIdOrderBySequenceNumber(org, id).stream()
        .map(GrantReportTemplateDtos.SectionResponse::from)
        .toList();
  }

  @Transactional
  public GrantReportTemplateDtos.SectionResponse updateSection(
      UUID org, UUID id, GrantReportTemplateDtos.UpdateSection r) {
    var s =
        sections
            .findByIdAndOrganizationId(id, org)
            .orElseThrow(() -> new ResourceNotFoundException("Grant report template section", id));
    s.update(
        r.title(),
        r.instructions(),
        r.sequenceNumber(),
        r.sectionType(),
        r.required(),
        r.maxLength());
    try {
      sections.flush();
      return GrantReportTemplateDtos.SectionResponse.from(s);
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException(
          "DUPLICATE_TEMPLATE_SECTION_SEQUENCE", "Template section sequence already exists");
    }
  }
}
