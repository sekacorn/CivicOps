package org.civicops.grantreporting.evidence;

import java.math.BigDecimal;
import java.util.UUID;

public record EvidenceDraft(
    EvidenceSourceModule sourceModule,
    String sourceType,
    UUID sourceId,
    GrantReportMetric metric,
    String label,
    EvidenceValueState state,
    BigDecimal numericValue,
    BigDecimal monetaryValue,
    String textValue,
    String unit,
    String sourceReference,
    String metadataJson) {
  public static EvidenceDraft number(
      EvidenceSourceModule m,
      String type,
      UUID id,
      GrantReportMetric metric,
      BigDecimal value,
      String unit,
      String ref) {
    return new EvidenceDraft(
        m, type, id, metric, null, EvidenceValueState.VERIFIED, value, null, null, unit, ref, null);
  }

  public static EvidenceDraft money(
      EvidenceSourceModule m,
      String type,
      UUID id,
      GrantReportMetric metric,
      BigDecimal value,
      String ref) {
    return new EvidenceDraft(
        m,
        type,
        id,
        metric,
        null,
        EvidenceValueState.VERIFIED,
        null,
        value,
        null,
        "USD",
        ref,
        null);
  }

  public static EvidenceDraft missing(
      EvidenceSourceModule m, GrantReportMetric metric, String ref) {
    return new EvidenceDraft(
        m,
        "AGGREGATE",
        null,
        metric,
        null,
        EvidenceValueState.MISSING,
        null,
        null,
        null,
        null,
        ref,
        null);
  }

  public static EvidenceDraft manual(
      String label,
      BigDecimal number,
      BigDecimal money,
      String text,
      String unit,
      String ref,
      String notes) {
    return new EvidenceDraft(
        EvidenceSourceModule.MANUAL,
        "MANUAL",
        null,
        GrantReportMetric.MANUAL_VALUE,
        label,
        EvidenceValueState.VERIFIED,
        number,
        money,
        text,
        unit,
        ref,
        notes);
  }
}
