package org.civicops.facilities;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.civicops.core.security.*;
import org.civicops.core.user.UserRepository;
import org.civicops.facilities.availability.*;
import org.civicops.facilities.facility.*;
import org.civicops.facilities.reporting.*;
import org.civicops.facilities.reservation.*;
import org.civicops.facilities.reservation.dto.*;
import org.civicops.facilities.security.FacilityAccessService;
import org.civicops.facilities.space.*;
import org.civicops.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({
  FacilityController.class,
  FacilitySpaceController.class,
  FacilityBlackoutController.class,
  FacilityReservationController.class,
  FacilityReportingController.class
})
@Import({
  CoreSecurityConfiguration.class,
  JwtAuthenticationFilter.class,
  SecurityErrorWriter.class,
  CurrentUserProvider.class,
  GlobalExceptionHandler.class
})
@TestPropertySource(properties = "civicops.security.jwt.secret=0123456789abcdef0123456789abcdef")
class FacilityEndpointSecurityMvcTest {
  @Autowired MockMvc mvc;
  @MockitoBean FacilityService facilities;
  @MockitoBean FacilitySpaceService spaces;
  @MockitoBean OperatingHoursService hours;
  @MockitoBean AvailabilityService availability;
  @MockitoBean FacilityBlackoutService blackouts;
  @MockitoBean FacilityReservationService reservations;
  @MockitoBean FacilityReportingService reports;
  @MockitoBean FacilityAccessService access;
  @MockitoBean JwtService jwt;
  @MockitoBean UserRepository users;

  @Test
  void unauthenticatedFacilityReadIs401() throws Exception {
    mvc.perform(get("/api/v1/organizations/{org}/facilities", UUID.randomUUID()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void managerCanCreateFacilitySpaceBlackoutAndApprove() throws Exception {
    UUID org = UUID.randomUUID(), facility = UUID.randomUUID(), reservation = UUID.randomUUID();
    when(access.userId()).thenReturn(UUID.randomUUID());
    mvc.perform(
            post("/api/v1/organizations/{o}/facilities", org)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(
                    "{\"name\":\"Center\",\"facilityType\":\"COMMUNITY_CENTER\",\"timezone\":\"America/New_York\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/organizations/{o}/facilities/{f}/spaces", org, facility)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"name\":\"Room\",\"capacity\":30}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/organizations/{o}/facility-blackouts", org)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(
                    "{\"facilityId\":\""
                        + facility
                        + "\",\"startDateTime\":\"2030-01-01T15:00:00Z\",\"endDateTime\":\"2030-01-01T16:00:00Z\",\"reason\":\"Cleaning\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/organizations/{o}/facility-reservations/{r}/approve", org, reservation)
                .with(authentication(auth())))
        .andExpect(status().isOk());
    verify(access, times(3)).requireFacilityManagement(org);
    verify(access).requireReservationApproval(org);
  }

  @Test
  void eventCoordinatorCanReadAvailabilityAndRequest() throws Exception {
    UUID org = UUID.randomUUID(), space = UUID.randomUUID();
    mvc.perform(
            get(
                    "/api/v1/organizations/{o}/facility-spaces/{s}/availability?start=2030-01-01T15:00:00Z&end=2030-01-01T16:00:00Z",
                    org,
                    space)
                .with(authentication(auth())))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/v1/organizations/{o}/facility-reservations", org)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(
                    "{\"facilitySpaceId\":\""
                        + space
                        + "\",\"title\":\"Event\",\"startDateTime\":\"2030-01-01T15:00:00Z\",\"endDateTime\":\"2030-01-01T16:00:00Z\"}"))
        .andExpect(status().isCreated());
    verify(access).requireFacilityRead(org);
    verify(access).requireReservationRequest(org);
  }

  @Test
  void viewerReadsPrivacySafeReservationSummary() throws Exception {
    UUID org = UUID.randomUUID();
    when(reservations.list(eq(org), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(
            new PageImpl<>(
                List.of(
                    new ReservationSummaryResponse(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Center",
                        UUID.randomUUID(),
                        "Room",
                        "Meeting",
                        Instant.now(),
                        Instant.now().plusSeconds(10),
                        10,
                        ReservationStatus.PENDING,
                        Instant.now(),
                        null))));
    mvc.perform(
            get("/api/v1/organizations/{o}/facility-reservations", org)
                .with(authentication(auth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].title").value("Meeting"))
        .andExpect(jsonPath("$.content[0].requesterEmail").doesNotExist())
        .andExpect(jsonPath("$.content[0].notes").doesNotExist());
  }

  @Test
  void managerReservationDetailIncludesRequesterContactAndPrivateNotes() throws Exception {
    UUID org = UUID.randomUUID(), reservation = UUID.randomUUID(), requester = UUID.randomUUID();
    when(reservations.detail(org, reservation)).thenReturn(detail(reservation, org, requester));
    mvc.perform(
            get("/api/v1/organizations/{o}/facility-reservations/{r}", org, reservation)
                .with(authentication(auth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requesterEmail").value("private@example.org"))
        .andExpect(jsonPath("$.notes").value("Private setup notes"));
    verify(access).requireOwnReservationAccess(org, requester);
  }

  @Test
  void selfServiceRequesterCanReadOwnReservationDetail() throws Exception {
    UUID org = UUID.randomUUID(), reservation = UUID.randomUUID(), requester = UUID.randomUUID();
    when(reservations.detail(org, reservation)).thenReturn(detail(reservation, org, requester));
    mvc.perform(
            get("/api/v1/organizations/{o}/facility-reservations/{r}", org, reservation)
                .with(authentication(auth())))
        .andExpect(status().isOk());
    verify(access).requireOwnReservationAccess(org, requester);
  }

  @Test
  void differentUserCannotReadReservationPrivateDetail() throws Exception {
    UUID org = UUID.randomUUID(), reservation = UUID.randomUUID(), requester = UUID.randomUUID();
    when(reservations.detail(org, reservation)).thenReturn(detail(reservation, org, requester));
    doThrow(new AccessDeniedException("not requester"))
        .when(access)
        .requireOwnReservationAccess(org, requester);
    mvc.perform(
            get("/api/v1/organizations/{o}/facility-reservations/{r}", org, reservation)
                .with(authentication(auth())))
        .andExpect(status().isForbidden());
  }

  @Test
  void viewerMutationReturns403() throws Exception {
    UUID org = UUID.randomUUID();
    doThrow(new AccessDeniedException("read only")).when(access).requireFacilityManagement(org);
    mvc.perform(
            post("/api/v1/organizations/{o}/facilities", org)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"name\":\"Center\",\"facilityType\":\"OTHER\",\"timezone\":\"UTC\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void volunteerAdministrativeMutationReturns403() throws Exception {
    UUID org = UUID.randomUUID();
    doThrow(new AccessDeniedException("no role")).when(access).requireReservationApproval(org);
    mvc.perform(
            post(
                    "/api/v1/organizations/{o}/facility-reservations/{r}/approve",
                    org,
                    UUID.randomUUID())
                .with(authentication(auth())))
        .andExpect(status().isForbidden());
  }

  @Test
  void crossOrganizationReadReturns403() throws Exception {
    UUID org = UUID.randomUUID();
    doThrow(new AccessDeniedException("cross org")).when(access).requireFacilityRead(org);
    mvc.perform(get("/api/v1/organizations/{o}/facilities", org).with(authentication(auth())))
        .andExpect(status().isForbidden());
  }

  @Test
  void unsupportedSortReturns400() throws Exception {
    UUID org = UUID.randomUUID();
    mvc.perform(
            get("/api/v1/organizations/{o}/facilities?sort=notes", org)
                .with(authentication(auth())))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/v1/organizations/{o}/facility-reservations?sort=requesterEmail", org)
                .with(authentication(auth())))
        .andExpect(status().isBadRequest());
  }

  private ReservationDetailResponse detail(UUID id, UUID org, UUID requester) {
    Instant start = Instant.parse("2030-01-01T15:00:00Z");
    return new ReservationDetailResponse(
        id,
        org,
        UUID.randomUUID(),
        "Center",
        UUID.randomUUID(),
        "Room",
        requester,
        "Requester",
        "private@example.org",
        null,
        "Meeting",
        "Community program",
        start,
        start.plusSeconds(3600),
        20,
        ReservationStatus.PENDING,
        start.minusSeconds(60),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "Private setup notes",
        start.minusSeconds(60),
        start.minusSeconds(60),
        0);
  }

  private UsernamePasswordAuthenticationToken auth() {
    return UsernamePasswordAuthenticationToken.authenticated(
        new CivicOpsPrincipal(UUID.randomUUID(), "facility@example.org"), null, List.of());
  }
}
