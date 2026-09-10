package org.civicops.cases.reporting.dto;

import java.util.UUID;

public record CaseWorkloadResponse(
    UUID userId, String displayName, long openCases, long inProgressCases, long onHoldCases) {}
