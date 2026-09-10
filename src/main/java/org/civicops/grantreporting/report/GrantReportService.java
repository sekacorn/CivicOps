package org.civicops.grantreporting.report;

import java.time.*;
import java.util.*;
import java.util.stream.*;
import org.civicops.core.user.UserService;
import org.civicops.grantreporting.evidence.*;
import org.civicops.grantreporting.narrative.GrantNarrativeProvider;
import org.civicops.grantreporting.provider.GrantReportEvidenceProvider;
import org.civicops.grantreporting.provider.GrantReportEvidenceProvider.EvidenceContext;
import org.civicops.grantreporting.template.*;
import org.civicops.grants.grant.GrantService;
import org.civicops.shared.exception.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GrantReportService {
  private static final Set<EvidenceSourceModule> DEFAULT_SOURCES =
      EnumSet.of(
          EvidenceSourceModule.GRANT,
          EvidenceSourceModule.EVENTS,
          EvidenceSourceModule.VOLUNTEERS,
          EvidenceSourceModule.DONATIONS);
  private final GrantReportRepository reports;
  private final GrantReportSectionRepository sections;
  private final GrantReportEvidenceRepository evidence;
  private final GrantReportTemplateService templates;
  private final GrantReportTemplateSectionRepository templateSections;
  private final GrantService grants;
  private final UserService users;
  private final List<GrantReportEvidenceProvider> providers;
  private final GrantNarrativeProvider narrative;
  private final Clock clock;

  public GrantReportService(
      GrantReportRepository r,
      GrantReportSectionRepository s,
      GrantReportEvidenceRepository e,
      GrantReportTemplateService t,
      GrantReportTemplateSectionRepository ts,
      GrantService g,
      UserService u,
      List<GrantReportEvidenceProvider> p,
      GrantNarrativeProvider n,
      Clock c) {
    reports = r;
    sections = s;
    evidence = e;
    templates = t;
    templateSections = ts;
    grants = g;
    users = u;
    providers = p;
    narrative = n;
    clock = c;
  }

  @Transactional
  public GrantReportDtos.Detail create(
      UUID org, UUID grantId, UUID actor, GrantReportDtos.Create r) {
    var grant = grants.require(org, grantId);
    var template = templates.require(org, r.templateId());
    template.requireUsableFor(grant);
    Set<EvidenceSourceModule> sources = normalize(r.selectedSources());
    GrantReport report =
        reports.save(
            new GrantReport(
                grant,
                template,
                r.reportingPeriodStart(),
                r.reportingPeriodEnd(),
                sources,
                users.requireEntity(actor)));
    seed(report, template);
    return GrantReportDtos.Detail.from(report);
  }

  @Transactional(readOnly = true)
  public GrantReport require(UUID org, UUID id) {
    return reports
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Grant report", id));
  }

  private GrantReport locked(UUID org, UUID id) {
    return reports
        .findLocked(org, id)
        .orElseThrow(() -> new ResourceNotFoundException("Grant report", id));
  }

  @Transactional(readOnly = true)
  public GrantReportDtos.Detail detail(UUID org, UUID id, boolean manager) {
    GrantReport r = require(org, id);
    if (!manager && r.getStatus() != GrantReportStatus.FINALIZED)
      throw new org.springframework.security.access.AccessDeniedException(
          "Program managers may read only finalized grant reports");
    return GrantReportDtos.Detail.from(r);
  }

  @Transactional(readOnly = true)
  public Page<GrantReportDtos.Summary> list(
      UUID org, UUID grant, GrantReportStatus status, boolean manager, Pageable p) {
    Specification<GrantReport> s = (r, q, c) -> c.equal(r.get("organization").get("id"), org);
    if (grant != null) s = s.and((r, q, c) -> c.equal(r.get("grant").get("id"), grant));
    if (status != null) s = s.and((r, q, c) -> c.equal(r.get("status"), status));
    if (!manager) s = s.and((r, q, c) -> c.equal(r.get("status"), GrantReportStatus.FINALIZED));
    return reports
        .findAll(s, p)
        .map(
            r ->
                new GrantReportDtos.Summary(
                    r.getId(),
                    r.getGrant().getId(),
                    r.getGrant().getGrantName(),
                    r.getTemplate().getId(),
                    r.getPeriodStart(),
                    r.getPeriodEnd(),
                    r.getStatus(),
                    r.getGeneratedAt(),
                    r.getFinalizedAt(),
                    r.getCreatedAt()));
  }

  @Transactional
  public GrantReportDtos.Detail update(UUID org, UUID id, GrantReportDtos.Update x) {
    GrantReport r = locked(org, id);
    GrantReportTemplate template =
        x.templateId() == null ? null : templates.require(org, x.templateId());
    if (template != null) template.requireUsableFor(r.getGrant());
    boolean changed = template != null && !template.getId().equals(r.getTemplate().getId());
    r.update(
        x.reportingPeriodStart(),
        x.reportingPeriodEnd(),
        template,
        x.selectedSources() == null ? null : normalize(x.selectedSources()));
    if (changed) {
      sections.deleteByReportId(id);
      sections.flush();
      seed(r, template);
    }
    return GrantReportDtos.Detail.from(r);
  }

  @Transactional
  public GrantReportDtos.Detail generate(UUID org, UUID id, boolean regenerate) {
    GrantReport r = locked(org, id);
    if (regenerate && r.getStatus() == GrantReportStatus.DRAFT)
      throw new BusinessRuleException(
          "REPORT_NOT_GENERATED", "Generate the draft before regenerating");
    if (!regenerate && r.getStatus() != GrantReportStatus.DRAFT)
      throw new ConflictException(
          "REPORT_ALREADY_GENERATED", "Use regenerate for an existing generation");
    r.requireMutable();
    evidence.deleteSystemEvidence(id);
    Instant now = Instant.now(clock);
    EvidenceContext context =
        new EvidenceContext(org, r.getGrant(), r.getPeriodStart(), r.getPeriodEnd());
    Map<EvidenceSourceModule, GrantReportEvidenceProvider> available =
        providers.stream()
            .collect(Collectors.toMap(GrantReportEvidenceProvider::sourceModule, p -> p));
    for (var source : r.sources()) {
      var provider = available.get(source);
      if (provider == null) continue;
      List<GrantReportEvidenceSnapshot> snapshots =
          provider.collect(context).stream()
              .map(d -> new GrantReportEvidenceSnapshot(r, d, now, null))
              .toList();
      evidence.saveAll(snapshots);
    }
    List<GrantReportEvidenceSnapshot> all = evidence.forReport(org, id);
    if (all.isEmpty())
      throw new BusinessRuleException(
          "REPORT_EVIDENCE_REQUIRED", "Report generation produced no evidence");
    String evidenceSummary =
        all.stream()
            .map(
                e ->
                    e.getMetricKey()
                        + "="
                        + (e.getValueState() == EvidenceValueState.VERIFIED
                            ? render(e)
                            : e.getValueState().name()))
            .collect(Collectors.joining("; "));
    for (var section : sections.findAllByOrganizationIdAndReportIdOrderBySequenceNumber(org, id))
      section.generate(narrative.generate(r, section, all), evidenceSummary, now);
    r.generated(now);
    return GrantReportDtos.Detail.from(r);
  }

  @Transactional
  public GrantReportDtos.SectionResponse editSection(UUID org, UUID id, String content) {
    GrantReportSection s = requireSection(org, id);
    s.edit(content);
    if (s.getReport().getStatus() == GrantReportStatus.GENERATED) s.getReport().underReview();
    return GrantReportDtos.SectionResponse.from(s);
  }

  @Transactional
  public GrantReportDtos.SectionResponse approveSection(UUID org, UUID id) {
    GrantReportSection s = requireSection(org, id);
    s.approve();
    if (s.getReport().getStatus() == GrantReportStatus.GENERATED) s.getReport().underReview();
    return GrantReportDtos.SectionResponse.from(s);
  }

  @Transactional(readOnly = true)
  public GrantReportSection requireSection(UUID org, UUID id) {
    return sections
        .findByIdAndOrganizationId(id, org)
        .orElseThrow(() -> new ResourceNotFoundException("Grant report section", id));
  }

  @Transactional(readOnly = true)
  public List<GrantReportDtos.SectionResponse> sectionList(UUID org, UUID report, boolean manager) {
    GrantReport r = require(org, report);
    if (!manager && r.getStatus() != GrantReportStatus.FINALIZED)
      throw new org.springframework.security.access.AccessDeniedException(
          "Program managers may read only finalized reports");
    return sections.findAllByOrganizationIdAndReportIdOrderBySequenceNumber(org, report).stream()
        .map(GrantReportDtos.SectionResponse::from)
        .toList();
  }

  @Transactional
  public GrantReportDtos.Detail finalizeReport(UUID org, UUID id, UUID actor) {
    GrantReport r = locked(org, id);
    if (sections.countByReportIdAndRequiredTrueAndStatusNot(id, ReportSectionStatus.APPROVED) > 0)
      throw new BusinessRuleException(
          "REQUIRED_SECTIONS_NOT_APPROVED", "All required sections must be approved");
    if (evidence.countByReportId(id) == 0)
      throw new BusinessRuleException(
          "REPORT_EVIDENCE_REQUIRED", "Finalization requires frozen evidence");
    r.finalize(users.requireEntity(actor), Instant.now(clock));
    return GrantReportDtos.Detail.from(r);
  }

  @Transactional
  public EvidenceDtos.Response addManual(
      UUID org, UUID id, UUID actor, EvidenceDtos.ManualEvidence x) {
    GrantReport r = locked(org, id);
    r.requireMutable();
    int values =
        (x.numericValue() != null ? 1 : 0)
            + (x.monetaryValue() != null ? 1 : 0)
            + (x.textValue() != null ? 1 : 0);
    if (values != 1)
      throw new BusinessRuleException(
          "MANUAL_EVIDENCE_VALUE_REQUIRED", "Provide exactly one manual evidence value");
    EvidenceDraft d =
        EvidenceDraft.manual(
            x.label(),
            x.numericValue(),
            x.monetaryValue(),
            x.textValue(),
            x.unit(),
            x.sourceReference(),
            x.notes());
    return EvidenceDtos.Response.from(
        evidence.save(
            new GrantReportEvidenceSnapshot(r, d, Instant.now(clock), users.requireEntity(actor))));
  }

  @Transactional(readOnly = true)
  public List<EvidenceDtos.Response> evidence(UUID org, UUID id, boolean manager) {
    GrantReport r = require(org, id);
    if (!manager && r.getStatus() != GrantReportStatus.FINALIZED)
      throw new org.springframework.security.access.AccessDeniedException(
          "Program managers may read only finalized evidence");
    return evidence.forReport(org, id).stream().map(EvidenceDtos.Response::from).toList();
  }

  @Transactional(readOnly = true)
  public List<EvidenceDtos.Missing> missing(UUID org, UUID id, boolean manager) {
    return evidence(org, id, manager).stream()
        .filter(e -> e.valueState() == EvidenceValueState.MISSING)
        .map(
            e ->
                new EvidenceDtos.Missing(
                    e.metricKey(),
                    e.metricLabel(),
                    e.sourceModule(),
                    e.metricLabel() + " was unavailable for this reporting period."))
        .toList();
  }

  private void seed(GrantReport r, GrantReportTemplate t) {
    var rows =
        templateSections.findAllByOrganizationIdAndTemplateIdOrderBySequenceNumber(
            r.getOrganization().getId(), t.getId());
    if (rows.isEmpty())
      throw new BusinessRuleException(
          "TEMPLATE_SECTIONS_REQUIRED", "Report template must contain at least one section");
    sections.saveAll(rows.stream().map(x -> new GrantReportSection(r, x)).toList());
  }

  private static Set<EvidenceSourceModule> normalize(Set<EvidenceSourceModule> s) {
    Set<EvidenceSourceModule> x =
        s == null || s.isEmpty() ? EnumSet.copyOf(DEFAULT_SOURCES) : EnumSet.copyOf(s);
    x.remove(EvidenceSourceModule.MANUAL);
    x.add(EvidenceSourceModule.GRANT);
    return x;
  }

  private static String render(GrantReportEvidenceSnapshot e) {
    if (e.getMonetaryValue() != null) return e.getMonetaryValue().toPlainString();
    if (e.getNumericValue() != null) return e.getNumericValue().toPlainString();
    return e.getTextValue();
  }
}
