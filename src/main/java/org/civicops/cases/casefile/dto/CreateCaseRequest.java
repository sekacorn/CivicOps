package org.civicops.cases.casefile.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;
import org.civicops.cases.casefile.*;

public record CreateCaseRequest(
    @NotNull UUID clientId,
    @NotBlank @Size(max = 100) String caseNumber,
    @NotBlank @Size(max = 200) String title,
    @Size(max = 10000) String description,
    @NotNull CaseType caseType,
    @NotNull CasePriority priority,
    LocalDate openedDate,
    @Size(max = 200) String programName,
    @Size(max = 200) String intakeSource) {}
