package org.civicops.cases.task.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;
import org.civicops.cases.casefile.CasePriority;

public record CreateCaseTaskRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 10000) String description,
    UUID assignedUserId,
    LocalDate dueDate,
    @NotNull CasePriority priority) {}
