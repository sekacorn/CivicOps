package org.civicops.cases.task.dto;

import java.time.*;
import java.util.UUID;
import org.civicops.cases.casefile.CasePriority;
import org.civicops.cases.task.*;

public record CaseTaskResponse(
    UUID id,
    UUID caseId,
    String title,
    String description,
    UUID assignedUserId,
    LocalDate dueDate,
    CaseTaskStatus status,
    CasePriority priority,
    boolean overdue,
    Instant completedAt,
    Instant createdAt,
    Instant updatedAt) {
  public static CaseTaskResponse from(CaseTask t, LocalDate today) {
    return new CaseTaskResponse(
        t.getId(),
        t.getCaseRecord().getId(),
        t.getTitle(),
        t.getDescription(),
        t.getAssignedUser() == null ? null : t.getAssignedUser().getId(),
        t.getDueDate(),
        t.getStatus(),
        t.getPriority(),
        t.isOverdue(today),
        t.getCompletedAt(),
        t.getCreatedAt(),
        t.getUpdatedAt());
  }
}
