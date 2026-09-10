package org.civicops.grantreporting.evidence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.grantreporting.report.GrantReport;
import org.civicops.shared.domain.BaseEntity;

@Entity
@Table(
    name = "grant_report_evidence_snapshot",
    uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "id"}))
public class GrantReportEvidenceSnapshot extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "report_id")
  private GrantReport report;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private EvidenceSourceModule sourceModule;

  @Column(nullable = false, length = 100)
  private String sourceType;

  private java.util.UUID sourceId;

  @Column(nullable = false, length = 100)
  private String metricKey;

  @Column(nullable = false, length = 200)
  private String metricLabel;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private EvidenceValueState valueState;

  @Column(precision = 19, scale = 3)
  private BigDecimal numericValue;

  @Column(precision = 19, scale = 2)
  private BigDecimal monetaryValue;

  @Column(columnDefinition = "TEXT")
  private String textValue;

  @Column(length = 50)
  private String unit;

  @Column(nullable = false)
  private LocalDate periodStart;

  @Column(nullable = false)
  private LocalDate periodEnd;

  @Column(nullable = false)
  private Instant capturedAt;

  @Column(nullable = false, length = 500)
  private String sourceReference;

  @Column(columnDefinition = "TEXT")
  private String metadataJson;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "entered_by_user_id")
  private User enteredBy;

  protected GrantReportEvidenceSnapshot() {}

  public GrantReportEvidenceSnapshot(GrantReport r, EvidenceDraft d, Instant at, User entered) {
    organization = r.getOrganization();
    report = r;
    sourceModule = d.sourceModule();
    sourceType = d.sourceType();
    sourceId = d.sourceId();
    metricKey = d.metric().key();
    metricLabel = d.label() == null ? d.metric().label() : d.label();
    valueState = d.state();
    numericValue = d.numericValue();
    monetaryValue = d.monetaryValue();
    textValue = d.textValue();
    unit = d.unit();
    periodStart = r.getPeriodStart();
    periodEnd = r.getPeriodEnd();
    capturedAt = at;
    sourceReference = d.sourceReference();
    metadataJson = d.metadataJson();
    enteredBy = entered;
  }

  public GrantReport getReport() {
    return report;
  }

  public EvidenceSourceModule getSourceModule() {
    return sourceModule;
  }

  public String getSourceType() {
    return sourceType;
  }

  public java.util.UUID getSourceId() {
    return sourceId;
  }

  public String getMetricKey() {
    return metricKey;
  }

  public String getMetricLabel() {
    return metricLabel;
  }

  public EvidenceValueState getValueState() {
    return valueState;
  }

  public BigDecimal getNumericValue() {
    return numericValue;
  }

  public BigDecimal getMonetaryValue() {
    return monetaryValue;
  }

  public String getTextValue() {
    return textValue;
  }

  public String getUnit() {
    return unit;
  }

  public LocalDate getPeriodStart() {
    return periodStart;
  }

  public LocalDate getPeriodEnd() {
    return periodEnd;
  }

  public Instant getCapturedAt() {
    return capturedAt;
  }

  public String getSourceReference() {
    return sourceReference;
  }

  public String getMetadataJson() {
    return metadataJson;
  }

  public User getEnteredBy() {
    return enteredBy;
  }
}
