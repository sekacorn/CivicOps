package org.civicops.core.membership.dto;

import jakarta.validation.constraints.NotNull;
import org.civicops.core.membership.Role;
import java.util.UUID;

public record CreateMembershipRequest(@NotNull UUID userId, @NotNull Role role) {
}
