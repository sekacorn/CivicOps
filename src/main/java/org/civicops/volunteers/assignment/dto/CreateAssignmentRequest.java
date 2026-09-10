package org.civicops.volunteers.assignment.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateAssignmentRequest(@NotNull UUID volunteerId) {}
