package org.civicops.donations.campaign.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateCampaignRequest(
    @Size(min = 1, max = 200) String name,
    String description,
    @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal goalAmount,
    LocalDate startDate,
    LocalDate endDate) {}
