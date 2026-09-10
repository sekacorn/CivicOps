package org.civicops.grantreporting.export;

import java.time.*;
import java.util.*;
import org.civicops.grantreporting.evidence.*;
import org.civicops.grantreporting.report.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GrantReportExportService {
  private final GrantReportService reports;

  public GrantReportExportService(GrantReportService r) {
    reports = r;
  }

  @Transactional(readOnly = true)
  public ExportDocument json(UUID org, UUID id, boolean manager) {
    GrantReport report = reports.require(org, id);
    if (!manager && report.getStatus() != GrantReportStatus.FINALIZED)
      throw new org.springframework.security.access.AccessDeniedException(
          "Only finalized reports are readable");
    return new ExportDocument(
        report.getId(),
        report.getGrant().getId(),
        report.getGrant().getGrantName(),
        report.getPeriodStart(),
        report.getPeriodEnd(),
        report.getStatus(),
        report.getGeneratedAt(),
        report.getFinalizedAt(),
        reports.sectionList(org, id, manager),
        reports.evidence(org, id, manager));
  }

  @Transactional(readOnly = true)
  public String markdown(UUID org, UUID id, boolean manager) {
    ExportDocument x = json(org, id, manager);
    StringBuilder b =
        new StringBuilder("# ")
            .append(x.grantName())
            .append(" Grant Report\n\n**Reporting period:** ")
            .append(x.periodStart())
            .append(" through ")
            .append(x.periodEnd())
            .append(" (inclusive)\n\n**Status:** ")
            .append(x.status())
            .append("\n\n");
    for (var s : x.sections())
      b.append("## ")
          .append(s.title())
          .append("\n\n")
          .append(
              s.finalContent() != null
                  ? s.finalContent()
                  : s.editedContent() != null ? s.editedContent() : s.generatedContent())
          .append("\n\n");
    b.append("## Evidence\n\n");
    for (var e : x.evidence())
      b.append("- **")
          .append(e.metricLabel())
          .append(":** ")
          .append(e.valueState() == EvidenceValueState.VERIFIED ? value(e) : e.valueState())
          .append(" — ")
          .append(e.sourceModule())
          .append("; ")
          .append(e.sourceReference())
          .append("; captured ")
          .append(e.capturedAt())
          .append("\n");
    return b.toString();
  }

  private static String value(EvidenceDtos.Response e) {
    if (e.monetaryValue() != null) return "$" + e.monetaryValue().toPlainString();
    if (e.numericValue() != null)
      return e.numericValue().stripTrailingZeros().toPlainString()
          + (e.unit() == null ? "" : " " + e.unit());
    return e.textValue();
  }

  public record ExportDocument(
      UUID reportId,
      UUID grantId,
      String grantName,
      LocalDate periodStart,
      LocalDate periodEnd,
      GrantReportStatus status,
      Instant generatedAt,
      Instant finalizedAt,
      List<GrantReportDtos.SectionResponse> sections,
      List<EvidenceDtos.Response> evidence) {}
}
