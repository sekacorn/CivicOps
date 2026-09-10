package org.civicops.events.reporting.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record EventReportResponse(
    UUID eventId,
    Integer capacity,
    long registered,
    long waitlisted,
    long attended,
    long noShow,
    long cancelled,
    Integer remainingCapacity,
    BigDecimal attendanceRate,
    UUID linkedGrantId,
    String linkedGrantName,
    UUID linkedCampaignId,
    String linkedCampaignName,
    BigDecimal campaignGoal,
    BigDecimal campaignRaised,
    long campaignDonationCount) {}
