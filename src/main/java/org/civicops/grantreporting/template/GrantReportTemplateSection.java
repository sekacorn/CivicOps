package org.civicops.grantreporting.template;

import jakarta.persistence.*;
import org.civicops.core.organization.Organization;
import org.civicops.shared.domain.BaseEntity;

@Entity
@Table(
    name = "grant_report_template_section",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"template_id", "section_key"}),
      @UniqueConstraint(columnNames = {"template_id", "sequence_number"}),
      @UniqueConstraint(columnNames = {"organization_id", "id"})
    })
public class GrantReportTemplateSection extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "template_id")
  private GrantReportTemplate template;

  @Column(name = "section_key", nullable = false, length = 100)
  private String sectionKey;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String instructions;

  @Column(name = "sequence_number", nullable = false)
  private int sequenceNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "section_type", nullable = false, length = 30)
  private ReportSectionType sectionType;

  @Column(nullable = false)
  private boolean required;

  @Column(name = "max_length")
  private Integer maxLength;

  protected GrantReportTemplateSection() {}

  public GrantReportTemplateSection(
      GrantReportTemplate t,
      String k,
      String title,
      String i,
      int seq,
      ReportSectionType type,
      boolean req,
      Integer max) {
    organization = t.getOrganization();
    template = t;
    sectionKey = k;
    this.title = title;
    instructions = i;
    sequenceNumber = seq;
    sectionType = type;
    required = req;
    maxLength = max;
  }

  public Organization getOrganization() {
    return organization;
  }

  public GrantReportTemplate getTemplate() {
    return template;
  }

  public String getSectionKey() {
    return sectionKey;
  }

  public String getTitle() {
    return title;
  }

  public String getInstructions() {
    return instructions;
  }

  public int getSequenceNumber() {
    return sequenceNumber;
  }

  public ReportSectionType getSectionType() {
    return sectionType;
  }

  public boolean isRequired() {
    return required;
  }

  public Integer getMaxLength() {
    return maxLength;
  }

  public void update(
      String title,
      String instructions,
      Integer sequence,
      ReportSectionType type,
      Boolean required,
      Integer max) {
    if (title != null) this.title = title;
    if (instructions != null) this.instructions = instructions;
    if (sequence != null) this.sequenceNumber = sequence;
    if (type != null) this.sectionType = type;
    if (required != null) this.required = required;
    if (max != null) this.maxLength = max;
  }
}
