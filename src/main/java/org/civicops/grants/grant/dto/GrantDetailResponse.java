package org.civicops.grants.grant.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.civicops.grants.grant.*;

public record GrantDetailResponse(
    UUID id,
    UUID organizationId,
    String grantName,
    String grantorName,
    String grantNumber,
    String description,
    BigDecimal awardAmount,
    LocalDate applicationDeadline,
    LocalDate submittedDate,
    LocalDate awardDate,
    LocalDate startDate,
    LocalDate endDate,
    LocalDate reportingDeadline,
    GrantStatus status,
    boolean restricted,
    String restrictionDescription,
    String primaryContactName,
    String primaryContactEmail,
    String notes,
    UUID createdByUserId,
    Instant createdAt,
    Instant updatedAt) {
  public static GrantDetailResponse from(Grant g) {
    return new GrantDetailResponse(
        g.getId(),
        g.getOrganization().getId(),
        g.getGrantName(),
        g.getGrantorName(),
        g.getGrantNumber(),
        g.getDescription(),
        g.getAwardAmount(),
        g.getApplicationDeadline(),
        g.getSubmittedDate(),
        g.getAwardDate(),
        g.getStartDate(),
        g.getEndDate(),
        g.getReportingDeadline(),
        g.getStatus(),
        g.isRestricted(),
        g.getRestrictionDescription(),
        g.getPrimaryContactName(),
        g.getPrimaryContactEmail(),
        g.getNotes(),
        g.getCreatedBy().getId(),
        g.getCreatedAt(),
        g.getUpdatedAt());
  }
}
