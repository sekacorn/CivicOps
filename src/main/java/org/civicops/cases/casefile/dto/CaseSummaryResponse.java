package org.civicops.cases.casefile.dto;

import java.time.*;
import java.util.UUID;
import org.civicops.cases.casefile.*;

public record CaseSummaryResponse(
    UUID id,
    String caseNumber,
    String title,
    CaseType caseType,
    CasePriority priority,
    CaseStatus status,
    LocalDate openedDate,
    LocalDate closedDate,
    UUID clientId,
    String clientDisplayName,
    UUID assignedUserId,
    Instant updatedAt) {
  public static CaseSummaryResponse from(CaseRecord c) {
    return new CaseSummaryResponse(
        c.getId(),
        c.getCaseNumber(),
        c.getTitle(),
        c.getCaseType(),
        c.getPriority(),
        c.getStatus(),
        c.getOpenedDate(),
        c.getClosedDate(),
        c.getClient().getId(),
        c.getClient().displayName(),
        c.getAssignedUser() == null ? null : c.getAssignedUser().getId(),
        c.getUpdatedAt());
  }
}
