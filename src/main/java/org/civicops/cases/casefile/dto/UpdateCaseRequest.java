package org.civicops.cases.casefile.dto;

import jakarta.validation.constraints.Size;
import org.civicops.cases.casefile.*;

public record UpdateCaseRequest(
    @Size(max = 200) String title,
    @Size(max = 10000) String description,
    CaseType caseType,
    CasePriority priority,
    @Size(max = 200) String programName,
    @Size(max = 200) String intakeSource) {}
