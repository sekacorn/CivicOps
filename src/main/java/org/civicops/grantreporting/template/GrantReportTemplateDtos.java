package org.civicops.grantreporting.template;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class GrantReportTemplateDtos {
  private GrantReportTemplateDtos() {}

  public record Create(
      @NotBlank @Size(max = 200) String name, @Size(max = 5000) String description, UUID grantId) {}

  public record Update(
      @Size(max = 200) String name, @Size(max = 5000) String description, Boolean active) {}

  public record Response(
      UUID id,
      UUID organizationId,
      UUID grantId,
      String name,
      String description,
      boolean active,
      UUID createdBy,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    public static Response from(GrantReportTemplate t) {
      return new Response(
          t.getId(),
          t.getOrganization().getId(),
          t.getGrant() == null ? null : t.getGrant().getId(),
          t.getName(),
          t.getDescription(),
          t.isActive(),
          t.getCreatedBy().getId(),
          t.getCreatedAt(),
          t.getUpdatedAt(),
          t.getVersion());
    }
  }

  public record AddSection(
      @NotBlank @Pattern(regexp = "^[a-z][a-z0-9_.-]*$") @Size(max = 100) String sectionKey,
      @NotBlank @Size(max = 200) String title,
      @Size(max = 10000) String instructions,
      @Min(1) int sequenceNumber,
      @NotNull ReportSectionType sectionType,
      boolean required,
      @Min(1) Integer maxLength) {}

  public record UpdateSection(
      @Size(max = 200) String title,
      @Size(max = 10000) String instructions,
      @Min(1) Integer sequenceNumber,
      ReportSectionType sectionType,
      Boolean required,
      @Min(1) Integer maxLength) {}

  public record SectionResponse(
      UUID id,
      UUID templateId,
      String sectionKey,
      String title,
      String instructions,
      int sequenceNumber,
      ReportSectionType sectionType,
      boolean required,
      Integer maxLength) {
    public static SectionResponse from(GrantReportTemplateSection s) {
      return new SectionResponse(
          s.getId(),
          s.getTemplate().getId(),
          s.getSectionKey(),
          s.getTitle(),
          s.getInstructions(),
          s.getSequenceNumber(),
          s.getSectionType(),
          s.isRequired(),
          s.getMaxLength());
    }
  }
}
