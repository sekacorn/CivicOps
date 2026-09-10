package org.civicops.grantreporting.report;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.grantreporting.evidence.EvidenceSourceModule;
import org.civicops.grantreporting.template.GrantReportTemplate;
import org.civicops.grants.grant.Grant;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(
    name = "grant_report",
    uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "id"}))
public class GrantReport extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "grant_id")
  private Grant grant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "template_id")
  private GrantReportTemplate template;

  @Column(name = "reporting_period_start", nullable = false)
  private LocalDate periodStart;

  @Column(name = "reporting_period_end", nullable = false)
  private LocalDate periodEnd;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private GrantReportStatus status = GrantReportStatus.DRAFT;

  @Column(name = "selected_sources", nullable = false, length = 500)
  private String selectedSources;

  private Instant generatedAt;
  private Instant finalizedAt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by_user_id", updatable = false)
  private User createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "finalized_by_user_id")
  private User finalizedBy;

  protected GrantReport() {}

  public GrantReport(
      Grant g,
      GrantReportTemplate t,
      LocalDate from,
      LocalDate to,
      Set<EvidenceSourceModule> s,
      User u) {
    if (to.isBefore(from))
      throw new BusinessRuleException("INVALID_REPORT_PERIOD", "Report period end precedes start");
    organization = g.getOrganization();
    grant = g;
    template = t;
    periodStart = from;
    periodEnd = to;
    selectedSources =
        s.stream()
            .filter(x -> x != EvidenceSourceModule.MANUAL)
            .map(Enum::name)
            .sorted()
            .collect(Collectors.joining(","));
    createdBy = u;
  }

  public Organization getOrganization() {
    return organization;
  }

  public Grant getGrant() {
    return grant;
  }

  public GrantReportTemplate getTemplate() {
    return template;
  }

  public LocalDate getPeriodStart() {
    return periodStart;
  }

  public LocalDate getPeriodEnd() {
    return periodEnd;
  }

  public GrantReportStatus getStatus() {
    return status;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public Instant getFinalizedAt() {
    return finalizedAt;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public User getFinalizedBy() {
    return finalizedBy;
  }

  public Set<EvidenceSourceModule> sources() {
    return selectedSources.isBlank()
        ? EnumSet.noneOf(EvidenceSourceModule.class)
        : Arrays.stream(selectedSources.split(","))
            .map(EvidenceSourceModule::valueOf)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(EvidenceSourceModule.class)));
  }

  public void update(
      LocalDate from, LocalDate to, GrantReportTemplate t, Set<EvidenceSourceModule> s) {
    if (status != GrantReportStatus.DRAFT) throw immutable();
    LocalDate f = from == null ? periodStart : from, e = to == null ? periodEnd : to;
    if (e.isBefore(f))
      throw new BusinessRuleException("INVALID_REPORT_PERIOD", "Report period end precedes start");
    periodStart = f;
    periodEnd = e;
    if (t != null) template = t;
    if (s != null)
      selectedSources =
          s.stream()
              .filter(x -> x != EvidenceSourceModule.MANUAL)
              .map(Enum::name)
              .sorted()
              .collect(Collectors.joining(","));
  }

  public void generated(Instant at) {
    if (status != GrantReportStatus.DRAFT
        && status != GrantReportStatus.GENERATED
        && status != GrantReportStatus.UNDER_REVIEW) throw immutable();
    generatedAt = at;
    status = GrantReportStatus.GENERATED;
  }

  public void underReview() {
    if (status != GrantReportStatus.GENERATED)
      throw new BusinessRuleException(
          "REPORT_NOT_GENERATED", "Only generated reports enter review");
    status = GrantReportStatus.UNDER_REVIEW;
  }

  public void finalize(User by, Instant at) {
    if (status != GrantReportStatus.GENERATED && status != GrantReportStatus.UNDER_REVIEW)
      throw new BusinessRuleException("REPORT_NOT_FINALIZABLE", "Report is not ready to finalize");
    status = GrantReportStatus.FINALIZED;
    finalizedBy = by;
    finalizedAt = at;
  }

  public void requireMutable() {
    if (status == GrantReportStatus.FINALIZED || status == GrantReportStatus.CANCELLED)
      throw immutable();
  }

  private BusinessRuleException immutable() {
    return new BusinessRuleException(
        "FINALIZED_REPORT_IMMUTABLE", "Finalized or cancelled reports are immutable");
  }
}
