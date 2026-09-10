package org.civicops.cases.casefile.dto;

import java.time.*;
import java.util.UUID;
import org.civicops.cases.casefile.*;
import org.civicops.cases.client.dto.ClientSummaryResponse;

public record CaseDetailResponse(
    UUID id,
    UUID organizationId,
    String caseNumber,
    String title,
    String description,
    CaseType caseType,
    CasePriority priority,
    CaseStatus status,
    LocalDate openedDate,
    LocalDate closedDate,
    ClientSummaryResponse client,
    UUID assignedUserId,
    String assignedUserDisplayName,
    String programName,
    String intakeSource,
    UUID createdByUserId,
    Instant createdAt,
    Instant updatedAt,
    long version) {
  public static CaseDetailResponse from(CaseRecord c) {
    return new CaseDetailResponse(
        c.getId(),
        c.getOrganization().getId(),
        c.getCaseNumber(),
        c.getTitle(),
        c.getDescription(),
        c.getCaseType(),
        c.getPriority(),
        c.getStatus(),
        c.getOpenedDate(),
        c.getClosedDate(),
        ClientSummaryResponse.from(c.getClient()),
        c.getAssignedUser() == null ? null : c.getAssignedUser().getId(),
        c.getAssignedUser() == null
            ? null
            : (c.getAssignedUser().getFirstName() + " " + c.getAssignedUser().getLastName()).trim(),
        c.getProgramName(),
        c.getIntakeSource(),
        c.getCreatedBy().getId(),
        c.getCreatedAt(),
        c.getUpdatedAt(),
        c.getVersion());
  }
}
