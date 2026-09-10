package org.civicops.facilities.reservation;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.*;
import org.civicops.facilities.reservation.dto.*;
import org.civicops.facilities.security.FacilityAccessService;
import org.civicops.shared.web.SafePageables;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/facility-reservations")
@Tag(name = "Facility Reservations")
public class FacilityReservationController {
  private final FacilityReservationService reservations;
  private final FacilityAccessService access;

  public FacilityReservationController(FacilityReservationService r, FacilityAccessService a) {
    reservations = r;
    access = a;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ReservationDetailResponse create(
      @PathVariable UUID organizationId, @Valid @RequestBody CreateReservationRequest r) {
    access.requireReservationRequest(organizationId);
    return reservations.create(organizationId, access.userId(), r);
  }

  @GetMapping
  public Page<ReservationSummaryResponse> list(
      @PathVariable UUID organizationId,
      @RequestParam(required = false) ReservationStatus status,
      @RequestParam(required = false) UUID facilityId,
      @RequestParam(required = false) UUID spaceId,
      @RequestParam(required = false) UUID requesterUserId,
      @RequestParam(required = false) UUID eventId,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to,
      @PageableDefault(size = 20, sort = "startDateTime") Pageable p) {
    access.requireReservationRead(organizationId);
    return reservations.list(
        organizationId,
        status,
        facilityId,
        spaceId,
        requesterUserId,
        eventId,
        from,
        to,
        SafePageables.allow(p, Set.of("startDateTime", "endDateTime", "requestedAt", "createdAt")));
  }

  @GetMapping("/me")
  public Page<ReservationSummaryResponse> mine(
      @PathVariable UUID organizationId,
      @PageableDefault(size = 20, sort = "startDateTime") Pageable p) {
    access.requireReservationRequest(organizationId);
    return reservations.mine(
        organizationId,
        access.userId(),
        SafePageables.allow(p, Set.of("startDateTime", "endDateTime", "requestedAt", "createdAt")));
  }

  @GetMapping("/{reservationId}")
  public ReservationDetailResponse detail(
      @PathVariable UUID organizationId, @PathVariable UUID reservationId) {
    ReservationDetailResponse response = reservations.detail(organizationId, reservationId);
    privateAccess(organizationId, response.requestedByUserId());
    return response;
  }

  @PatchMapping("/{reservationId}")
  public ReservationDetailResponse update(
      @PathVariable UUID organizationId,
      @PathVariable UUID reservationId,
      @Valid @RequestBody UpdateReservationRequest r) {
    ReservationDetailResponse current = reservations.detail(organizationId, reservationId);
    privateAccess(organizationId, current.requestedByUserId());
    return reservations.update(organizationId, reservationId, r);
  }

  @PostMapping("/{reservationId}/approve")
  public ReservationDetailResponse approve(
      @PathVariable UUID organizationId, @PathVariable UUID reservationId) {
    access.requireReservationApproval(organizationId);
    return reservations.approve(organizationId, reservationId, access.userId());
  }

  @PostMapping("/{reservationId}/reject")
  public ReservationDetailResponse reject(
      @PathVariable UUID organizationId,
      @PathVariable UUID reservationId,
      @Valid @RequestBody ReservationDecisionRequest r) {
    access.requireReservationApproval(organizationId);
    return reservations.reject(organizationId, reservationId, access.userId(), r.reason());
  }

  @PostMapping("/{reservationId}/cancel")
  public ReservationDetailResponse cancel(
      @PathVariable UUID organizationId,
      @PathVariable UUID reservationId,
      @RequestBody(required = false) @Valid ReservationCancellationRequest r) {
    ReservationDetailResponse current = reservations.detail(organizationId, reservationId);
    privateAccess(organizationId, current.requestedByUserId());
    return reservations.cancel(organizationId, reservationId, r == null ? null : r.reason());
  }

  @PostMapping("/{reservationId}/complete")
  public ReservationDetailResponse complete(
      @PathVariable UUID organizationId, @PathVariable UUID reservationId) {
    access.requireReservationApproval(organizationId);
    return reservations.complete(organizationId, reservationId);
  }

  private void privateAccess(UUID org, UUID requester) {
    if (requester == null) access.requireFacilityManagement(org);
    else access.requireOwnReservationAccess(org, requester);
  }
}
