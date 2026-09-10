package org.civicops.cases.service.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.civicops.cases.service.*;

public record CaseServiceResponse(
    UUID id,
    UUID caseId,
    CaseServiceType serviceType,
    String description,
    LocalDate serviceDate,
    BigDecimal quantity,
    String unit,
    BigDecimal valueAmount,
    UUID providedByUserId,
    String notes,
    Instant createdAt) {
  public static CaseServiceResponse from(CaseServiceRecord r) {
    return new CaseServiceResponse(
        r.getId(),
        r.getCaseRecord().getId(),
        r.getServiceType(),
        r.getDescription(),
        r.getServiceDate(),
        r.getQuantity(),
        r.getUnit(),
        r.getValueAmount(),
        r.getProvidedBy() == null ? null : r.getProvidedBy().getId(),
        r.getNotes(),
        r.getCreatedAt());
  }
}
