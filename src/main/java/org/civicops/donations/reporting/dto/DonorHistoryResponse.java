package org.civicops.donations.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.civicops.donations.donation.dto.DonationSummaryResponse;
import org.springframework.data.domain.Page;

public record DonorHistoryResponse(
    UUID donorId,
    long donationCount,
    BigDecimal totalContributed,
    LocalDate mostRecentDonationDate,
    Page<DonationSummaryResponse> donations) {}
