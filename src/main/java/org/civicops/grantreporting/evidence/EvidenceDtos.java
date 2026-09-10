package org.civicops.grantreporting.evidence;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public final class EvidenceDtos {
  private EvidenceDtos() {}

  public record ManualEvidence(
      @NotBlank @Size(max = 200) String label,
      BigDecimal numericValue,
      BigDecimal monetaryValue,
      @Size(max = 10000) String textValue,
      @Size(max = 50) String unit,
      @NotBlank @Size(max = 500) String sourceReference,
      @Size(max = 5000) String notes) {}

  public record Response(
      UUID id,
      EvidenceSourceModule sourceModule,
      String sourceType,
      UUID sourceId,
      String metricKey,
      String metricLabel,
      EvidenceValueState valueState,
      BigDecimal numericValue,
      BigDecimal monetaryValue,
      String textValue,
      String unit,
      LocalDate periodStart,
      LocalDate periodEnd,
      Instant capturedAt,
      String sourceReference,
      String metadataJson,
      UUID enteredBy) {
    public static Response from(GrantReportEvidenceSnapshot e) {
      return new Response(
          e.getId(),
          e.getSourceModule(),
          e.getSourceType(),
          e.getSourceId(),
          e.getMetricKey(),
          e.getMetricLabel(),
          e.getValueState(),
          e.getNumericValue(),
          e.getMonetaryValue(),
          e.getTextValue(),
          e.getUnit(),
          e.getPeriodStart(),
          e.getPeriodEnd(),
          e.getCapturedAt(),
          e.getSourceReference(),
          e.getMetadataJson(),
          e.getEnteredBy() == null ? null : e.getEnteredBy().getId());
    }
  }

  public record Missing(
      String metricKey, String metricLabel, EvidenceSourceModule sourceModule, String message) {}
}
