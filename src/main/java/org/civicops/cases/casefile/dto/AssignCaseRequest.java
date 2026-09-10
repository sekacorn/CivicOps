package org.civicops.cases.casefile.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignCaseRequest(@NotNull UUID userId) {}
