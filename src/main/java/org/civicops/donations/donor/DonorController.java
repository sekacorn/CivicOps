package org.civicops.donations.donor;

import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.civicops.donations.donor.dto.*;
import org.civicops.donations.security.DonationAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/donors")
@Tag(name = "Donation Donors")
public class DonorController {
  private final DonorService service;
  private final DonationAccessService access;

  public DonorController(DonorService service, DonationAccessService access) {
    this.service = service;
    this.access = access;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public DonorDetailResponse create(
      @PathVariable UUID organizationId, @Valid @RequestBody CreateDonorRequest r) {
    access.requireManage(organizationId);
    return service.create(organizationId, r);
  }

  @GetMapping
  @Operation(
      summary = "List privacy-safe donor summaries",
      description =
          "Filters: type, normalized email, anonymous. Sort: lastName, organizationName, createdAt.")
  public Page<DonorSummaryResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) DonorType type,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) Boolean anonymous,
      @PageableDefault(size = 20, sort = "createdAt") Pageable p) {
    access.requireSummaryRead(organizationId);
    return service.list(
        organizationId,
        type,
        email,
        anonymous,
        SafePageables.allow(p, Set.of("lastName", "organizationName", "createdAt")));
  }

  @GetMapping("/{donorId}")
  public DonorDetailResponse detail(@PathVariable UUID organizationId, @PathVariable UUID donorId) {
    access.requireManage(organizationId);
    return service.detail(organizationId, donorId);
  }

  @PatchMapping("/{donorId}")
  @Operation(summary = "Update donor contact metadata using CivicOps omitted-field PATCH semantics")
  public DonorDetailResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID donorId,
      @Valid @RequestBody UpdateDonorRequest r) {
    access.requireManage(organizationId);
    return service.update(organizationId, donorId, r);
  }
}
