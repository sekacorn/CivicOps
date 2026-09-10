package org.civicops.grantreporting.report;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.civicops.grantreporting.evidence.EvidenceSourceModule;
import org.civicops.grantreporting.template.ReportSectionType;

public final class GrantReportDtos {
  private GrantReportDtos() {}

  public record Create(
      @NotNull UUID templateId,
      @NotNull LocalDate reportingPeriodStart,
      @NotNull LocalDate reportingPeriodEnd,
      Set<EvidenceSourceModule> selectedSources) {}

  public record Update(
      UUID templateId,
      LocalDate reportingPeriodStart,
      LocalDate reportingPeriodEnd,
      Set<EvidenceSourceModule> selectedSources) {}

  public record Summary(
      UUID id,
      UUID grantId,
      String grantName,
      UUID templateId,
      LocalDate reportingPeriodStart,
      LocalDate reportingPeriodEnd,
      GrantReportStatus status,
      Instant generatedAt,
      Instant finalizedAt,
      Instant createdAt) {}

  public record Detail(
      UUID id,
      UUID organizationId,
      UUID grantId,
      String grantName,
      UUID templateId,
      String templateName,
      LocalDate reportingPeriodStart,
      LocalDate reportingPeriodEnd,
      Set<EvidenceSourceModule> selectedSources,
      GrantReportStatus status,
      Instant generatedAt,
      Instant finalizedAt,
      UUID createdBy,
      UUID finalizedBy,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    public static Detail from(GrantReport r) {
      return new Detail(
          r.getId(),
          r.getOrganization().getId(),
          r.getGrant().getId(),
          r.getGrant().getGrantName(),
          r.getTemplate().getId(),
          r.getTemplate().getName(),
          r.getPeriodStart(),
          r.getPeriodEnd(),
          r.sources(),
          r.getStatus(),
          r.getGeneratedAt(),
          r.getFinalizedAt(),
          r.getCreatedBy().getId(),
          r.getFinalizedBy() == null ? null : r.getFinalizedBy().getId(),
          r.getCreatedAt(),
          r.getUpdatedAt(),
          r.getVersion());
    }
  }

  public record EditSection(@NotBlank @Size(max = 100000) String editedContent) {}

  public record SectionResponse(
      UUID id,
      UUID reportId,
      UUID templateSectionId,
      int sequenceNumber,
      String title,
      ReportSectionType sectionType,
      boolean required,
      String generatedContent,
      String editedContent,
      String finalContent,
      String evidenceSummary,
      ReportSectionStatus status,
      Instant generatedAt,
      long version) {
    public static SectionResponse from(GrantReportSection s) {
      return new SectionResponse(
          s.getId(),
          s.getReport().getId(),
          s.getTemplateSection().getId(),
          s.getSequenceNumber(),
          s.getTitle(),
          s.getSectionType(),
          s.isRequired(),
          s.getGeneratedContent(),
          s.getEditedContent(),
          s.getFinalContent(),
          s.getEvidenceSummary(),
          s.getStatus(),
          s.getGeneratedAt(),
          s.getVersion());
    }
  }
}
