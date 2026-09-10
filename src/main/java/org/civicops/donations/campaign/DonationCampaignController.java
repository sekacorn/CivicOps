package org.civicops.donations.campaign;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.civicops.donations.campaign.dto.*;
import org.civicops.donations.security.DonationAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/donation-campaigns")
@Tag(name = "Donation Campaigns")
public class DonationCampaignController {
  private final DonationCampaignService service;
  private final DonationAccessService access;

  public DonationCampaignController(DonationCampaignService service, DonationAccessService access) {
    this.service = service;
    this.access = access;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CampaignResponse create(
      @PathVariable UUID organizationId, @Valid @RequestBody CreateCampaignRequest r) {
    access.requireManage(organizationId);
    return service.create(organizationId, access.userId(), r);
  }

  @GetMapping
  @Operation(
      summary = "List campaigns",
      description = "Filters: status, from, to. Sort: name, startDate, endDate, createdAt.")
  public Page<CampaignResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) CampaignStatus status,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @PageableDefault(size = 20, sort = "name") Pageable p) {
    access.requireSummaryRead(organizationId);
    return service.list(
        organizationId,
        status,
        from,
        to,
        SafePageables.allow(p, Set.of("name", "startDate", "endDate", "createdAt")));
  }

  @GetMapping("/{campaignId}")
  public CampaignResponse detail(@PathVariable UUID organizationId, @PathVariable UUID campaignId) {
    access.requireSummaryRead(organizationId);
    return service.detail(organizationId, campaignId);
  }

  @PatchMapping("/{campaignId}")
  public CampaignResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID campaignId,
      @Valid @RequestBody UpdateCampaignRequest r) {
    access.requireManage(organizationId);
    return service.update(organizationId, campaignId, r);
  }

  @PostMapping("/{campaignId}/activate")
  public CampaignResponse activate(
      @PathVariable UUID organizationId, @PathVariable UUID campaignId) {
    return transition(organizationId, campaignId, CampaignStatus.ACTIVE);
  }

  @PostMapping("/{campaignId}/close")
  public CampaignResponse close(@PathVariable UUID organizationId, @PathVariable UUID campaignId) {
    return transition(organizationId, campaignId, CampaignStatus.CLOSED);
  }

  @PostMapping("/{campaignId}/cancel")
  public CampaignResponse cancel(@PathVariable UUID organizationId, @PathVariable UUID campaignId) {
    return transition(organizationId, campaignId, CampaignStatus.CANCELLED);
  }

  private CampaignResponse transition(UUID org, UUID id, CampaignStatus target) {
    access.requireManage(org);
    return service.transition(org, id, target);
  }
}
