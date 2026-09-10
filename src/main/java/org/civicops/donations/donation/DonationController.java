package org.civicops.donations.donation;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.civicops.donations.donation.dto.*;
import org.civicops.donations.security.DonationAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/donations")
@Tag(name = "Donations")
public class DonationController {
  private final DonationService service;
  private final DonationAccessService access;

  public DonationController(DonationService service, DonationAccessService access) {
    this.service = service;
    this.access = access;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record an immutable monetary or in-kind donation")
  public DonationResponse create(
      @PathVariable UUID organizationId, @Valid @RequestBody CreateDonationRequest r) {
    access.requireManage(organizationId);
    return service.create(organizationId, access.userId(), r);
  }

  @GetMapping
  @Operation(
      summary = "List donation summaries",
      description =
          "Filters: donorId, campaignId, paymentMethod, restricted, from, to. Sort: donationDate, amount, createdAt.")
  public Page<DonationSummaryResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) UUID donorId,
      @RequestParam(required = false) UUID campaignId,
      @RequestParam(required = false) DonationPaymentMethod paymentMethod,
      @RequestParam(required = false) Boolean restricted,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @PageableDefault(size = 20, sort = "donationDate") Pageable p) {
    access.requireSummaryRead(organizationId);
    return service.list(
        organizationId,
        donorId,
        campaignId,
        paymentMethod,
        restricted,
        from,
        to,
        SafePageables.allow(p, Set.of("donationDate", "amount", "createdAt")));
  }

  @GetMapping("/{donationId}")
  public DonationResponse detail(@PathVariable UUID organizationId, @PathVariable UUID donationId) {
    access.requireManage(organizationId);
    return service.detail(organizationId, donationId);
  }

  @PostMapping("/{donationId}/reverse")
  @Operation(summary = "Reverse a donation without deleting financial history")
  public DonationResponse reverse(
      @PathVariable UUID organizationId,
      @PathVariable UUID donationId,
      @Valid @RequestBody ReverseDonationRequest r) {
    access.requireManage(organizationId);
    return service.reverse(organizationId, donationId, access.userId(), r.reason());
  }
}
