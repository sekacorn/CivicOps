package org.civicops.core.security;

import java.util.UUID;

public record CivicOpsPrincipal(UUID userId, String email) {}
