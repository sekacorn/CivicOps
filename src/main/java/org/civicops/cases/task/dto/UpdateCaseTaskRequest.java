package org.civicops.cases.task.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import org.civicops.cases.casefile.CasePriority;
import org.civicops.cases.task.CaseTaskStatus;

public record UpdateCaseTaskRequest(
    @Size(max = 200) String title,
    @Size(max = 10000) String description,
    UUID assignedUserId,
    LocalDate dueDate,
    CasePriority priority,
    CaseTaskStatus status) {}
