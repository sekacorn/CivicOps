package org.civicops.core.membership.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.civicops.core.membership.Role;

public record CreateMembershipRequest(@NotNull UUID userId, @NotNull Role role) {}
