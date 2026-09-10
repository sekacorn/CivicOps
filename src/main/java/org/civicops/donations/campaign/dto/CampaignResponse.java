package org.civicops.donations.campaign.dto;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import org.civicops.donations.campaign.*;

public record CampaignResponse(
    UUID id,
    UUID organizationId,
    String name,
    String description,
    BigDecimal goalAmount,
    LocalDate startDate,
    LocalDate endDate,
    CampaignStatus status,
    UUID createdByUserId,
    Instant createdAt,
    Instant updatedAt) {
  public static CampaignResponse from(DonationCampaign c) {
    return new CampaignResponse(
        c.getId(),
        c.getOrganization().getId(),
        c.getName(),
        c.getDescription(),
        c.getGoalAmount(),
        c.getStartDate(),
        c.getEndDate(),
        c.getStatus(),
        c.getCreatedBy().getId(),
        c.getCreatedAt(),
        c.getUpdatedAt());
  }
}
