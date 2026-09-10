package org.civicops.donations.reporting;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.*;
import org.civicops.donations.reporting.dto.*;
import org.civicops.donations.security.DonationAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}")
@Tag(name = "Donation Reporting")
public class DonationReportingController {
  private final DonationReportingService service;
  private final DonationAccessService access;

  public DonationReportingController(
      DonationReportingService service, DonationAccessService access) {
    this.service = service;
    this.access = access;
  }

  @GetMapping("/donation-reports/summary")
  @Operation(summary = "Donation summary for an arbitrary inclusive date range")
  public DonationReportSummaryResponse summary(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    access.requireSummaryRead(organizationId);
    return service.summary(organizationId, from, to);
  }

  @GetMapping("/donation-reports/by-payment-method")
  public List<PaymentMethodTotalResponse> payment(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    access.requireSummaryRead(organizationId);
    return service.byPayment(organizationId, from, to);
  }

  @GetMapping("/donation-reports/by-campaign")
  public List<CampaignFinancialSummaryResponse> campaigns(@PathVariable UUID organizationId) {
    access.requireSummaryRead(organizationId);
    return service.byCampaign(organizationId);
  }

  @GetMapping("/donation-campaigns/{campaignId}/financial-summary")
  public CampaignFinancialSummaryResponse campaign(
      @PathVariable UUID organizationId, @PathVariable UUID campaignId) {
    access.requireSummaryRead(organizationId);
    return service.campaign(organizationId, campaignId);
  }

  @GetMapping("/donors/{donorId}/donations")
  public DonorHistoryResponse donor(
      @PathVariable UUID organizationId,
      @PathVariable UUID donorId,
      @PageableDefault(size = 20, sort = "donationDate", direction = Sort.Direction.DESC)
          Pageable p) {
    access.requireManage(organizationId);
    return service.donorHistory(
        organizationId,
        donorId,
        SafePageables.allow(p, Set.of("donationDate", "amount", "createdAt")));
  }
}
