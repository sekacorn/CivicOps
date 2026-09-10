package org.civicops.grants.grant.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.civicops.grants.grant.*;

public record GrantSummaryResponse(
    UUID id,
    String grantName,
    String grantorName,
    String grantNumber,
    BigDecimal awardAmount,
    LocalDate startDate,
    LocalDate endDate,
    LocalDate reportingDeadline,
    GrantStatus status,
    boolean restricted,
    Instant createdAt,
    Instant updatedAt) {
  public static GrantSummaryResponse from(Grant g) {
    return new GrantSummaryResponse(
        g.getId(),
        g.getGrantName(),
        g.getGrantorName(),
        g.getGrantNumber(),
        g.getAwardAmount(),
        g.getStartDate(),
        g.getEndDate(),
        g.getReportingDeadline(),
        g.getStatus(),
        g.isRestricted(),
        g.getCreatedAt(),
        g.getUpdatedAt());
  }
}
