package org.civicops.grantreporting.report;

import jakarta.persistence.*;
import java.time.Instant;
import org.civicops.core.organization.Organization;
import org.civicops.grantreporting.template.*;
import org.civicops.shared.domain.BaseEntity;
import org.civicops.shared.exception.BusinessRuleException;

@Entity
@Table(
    name = "grant_report_section",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"report_id", "template_section_id"}),
      @UniqueConstraint(columnNames = {"report_id", "sequence_number"}),
      @UniqueConstraint(columnNames = {"organization_id", "id"})
    })
public class GrantReportSection extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "report_id")
  private GrantReport report;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "template_section_id")
  private GrantReportTemplateSection templateSection;

  private int sequenceNumber;

  @Column(nullable = false, length = 200)
  private String title;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ReportSectionType sectionType;

  @Column(nullable = false)
  private boolean required;

  private Integer maxLength;

  @Column(columnDefinition = "TEXT")
  private String generatedContent;

  @Column(columnDefinition = "TEXT")
  private String editedContent;

  @Column(columnDefinition = "TEXT")
  private String finalContent;

  @Column(columnDefinition = "TEXT")
  private String evidenceSummary;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ReportSectionStatus status = ReportSectionStatus.PENDING;

  private Instant generatedAt;

  protected GrantReportSection() {}

  public GrantReportSection(GrantReport r, GrantReportTemplateSection t) {
    organization = r.getOrganization();
    report = r;
    templateSection = t;
    sequenceNumber = t.getSequenceNumber();
    title = t.getTitle();
    sectionType = t.getSectionType();
    required = t.isRequired();
    maxLength = t.getMaxLength();
  }

  public GrantReport getReport() {
    return report;
  }

  public GrantReportTemplateSection getTemplateSection() {
    return templateSection;
  }

  public int getSequenceNumber() {
    return sequenceNumber;
  }

  public String getTitle() {
    return title;
  }

  public ReportSectionType getSectionType() {
    return sectionType;
  }

  public boolean isRequired() {
    return required;
  }

  public String getGeneratedContent() {
    return generatedContent;
  }

  public String getEditedContent() {
    return editedContent;
  }

  public String getFinalContent() {
    return finalContent;
  }

  public String getEvidenceSummary() {
    return evidenceSummary;
  }

  public ReportSectionStatus getStatus() {
    return status;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public void generate(String content, String evidence, Instant at) {
    report.requireMutable();
    generatedContent = content;
    editedContent = null;
    finalContent = null;
    evidenceSummary = evidence;
    generatedAt = at;
    status = ReportSectionStatus.GENERATED;
  }

  public void edit(String content) {
    report.requireMutable();
    if (status == ReportSectionStatus.PENDING)
      throw new BusinessRuleException("SECTION_NOT_GENERATED", "Generate section before editing");
    if (maxLength != null && content.length() > maxLength)
      throw new BusinessRuleException(
          "SECTION_TOO_LONG", "Edited content exceeds template maximum");
    editedContent = content;
    finalContent = null;
    status = ReportSectionStatus.EDITED;
  }

  public void approve() {
    report.requireMutable();
    if (status != ReportSectionStatus.GENERATED && status != ReportSectionStatus.EDITED)
      throw new BusinessRuleException(
          "SECTION_NOT_APPROVABLE", "Section is not ready for approval");
    finalContent = editedContent == null ? generatedContent : editedContent;
    status = ReportSectionStatus.APPROVED;
  }

  public String effectiveContent() {
    return finalContent != null
        ? finalContent
        : editedContent != null ? editedContent : generatedContent;
  }
}
