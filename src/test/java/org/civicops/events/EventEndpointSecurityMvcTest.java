package org.civicops.events;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.*;
import org.civicops.core.security.*;
import org.civicops.core.user.UserRepository;
import org.civicops.events.event.*;
import org.civicops.events.event.dto.*;
import org.civicops.events.registration.*;
import org.civicops.events.registration.dto.*;
import org.civicops.events.reporting.*;
import org.civicops.events.reporting.dto.*;
import org.civicops.events.security.EventAccessService;
import org.civicops.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({
  EventController.class,
  EventRegistrationController.class,
  EventReportingController.class
})
@Import({
  CoreSecurityConfiguration.class,
  JwtAuthenticationFilter.class,
  SecurityErrorWriter.class,
  CurrentUserProvider.class,
  GlobalExceptionHandler.class
})
@TestPropertySource(properties = {"civicops.security.jwt.secret=0123456789abcdef0123456789abcdef"})
class EventEndpointSecurityMvcTest {
  @Autowired MockMvc mvc;
  @MockitoBean EventService events;
  @MockitoBean EventRegistrationService registrations;
  @MockitoBean EventReportingService reporting;
  @MockitoBean EventAccessService access;
  @MockitoBean JwtService jwt;
  @MockitoBean UserRepository users;

  @Test
  void unauthenticatedEventReadReturns401() throws Exception {
    mvc.perform(get("/api/v1/organizations/{org}/events", UUID.randomUUID()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void coordinatorCanCreateEvent() throws Exception {
    UUID org = UUID.randomUUID();
    when(access.userId()).thenReturn(UUID.randomUUID());
    mvc.perform(
            post("/api/v1/organizations/{org}/events", org)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(eventJson()))
        .andExpect(status().isCreated());
    verify(access).requireManage(org);
  }

  @Test
  void viewerCannotMutateEvent() throws Exception {
    UUID org = UUID.randomUUID();
    doThrow(new AccessDeniedException("read only")).when(access).requireManage(org);
    mvc.perform(
            post("/api/v1/organizations/{org}/events", org)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(eventJson()))
        .andExpect(status().isForbidden());
  }

  @Test
  void viewerCanReadSummary() throws Exception {
    UUID org = UUID.randomUUID();
    when(events.list(eq(org), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Page.empty());
    mvc.perform(get("/api/v1/organizations/{org}/events", org).with(authentication(auth())))
        .andExpect(status().isOk());
    verify(access).requireRead(org);
  }

  @Test
  void crossOrganizationReadReturns403() throws Exception {
    UUID org = UUID.randomUUID();
    doThrow(new AccessDeniedException("cross org")).when(access).requireRead(org);
    mvc.perform(get("/api/v1/organizations/{org}/events", org).with(authentication(auth())))
        .andExpect(status().isForbidden());
  }

  @Test
  void coordinatorCanManageExternalRegistration() throws Exception {
    UUID org = UUID.randomUUID(), event = UUID.randomUUID();
    mvc.perform(
            post("/api/v1/organizations/{org}/events/{event}/registrations", org, event)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"attendeeName\":\"Ada\",\"attendeeEmail\":\"ada@example.org\"}"))
        .andExpect(status().isCreated());
    verify(access).requireManage(org);
  }

  @Test
  void volunteerSelfRegistrationUsesSelfServiceBoundary() throws Exception {
    UUID org = UUID.randomUUID(), event = UUID.randomUUID(), user = UUID.randomUUID();
    when(access.userId()).thenReturn(user);
    mvc.perform(
            post("/api/v1/organizations/{org}/events/{event}/registrations/me", org, event)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isCreated());
    verify(access).requireSelfService(org);
    verify(registrations).register(eq(org), eq(event), eq(user), any());
  }

  @Test
  void volunteerCannotReadPrivateRegistration() throws Exception {
    UUID org = UUID.randomUUID(), id = UUID.randomUUID();
    doThrow(new AccessDeniedException("private")).when(access).requireRegistrationRead(org);
    mvc.perform(
            get("/api/v1/organizations/{org}/event-registrations/{id}", org, id)
                .with(authentication(auth())))
        .andExpect(status().isForbidden());
  }

  @Test
  void registrationSummariesDoNotSerializeContactFields() throws Exception {
    UUID org = UUID.randomUUID(), event = UUID.randomUUID();
    when(registrations.list(eq(org), eq(event), any(), any(), any(), any(), any()))
        .thenReturn(
            new PageImpl<>(
                List.of(
                    new RegistrationSummaryResponse(
                        UUID.randomUUID(), "Ada", RegistrationStatus.REGISTERED, Instant.now()))));
    mvc.perform(
            get("/api/v1/organizations/{org}/events/{event}/registrations", org, event)
                .with(authentication(auth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].attendeeName").value("Ada"))
        .andExpect(jsonPath("$.content[0].attendeeEmail").doesNotExist())
        .andExpect(jsonPath("$.content[0].attendeePhone").doesNotExist());
  }

  @Test
  void unsupportedEventSortReturns400() throws Exception {
    mvc.perform(
            get("/api/v1/organizations/{org}/events?sort=description", UUID.randomUUID())
                .with(authentication(auth())))
        .andExpect(status().isBadRequest());
  }

  private String eventJson() {
    return "{\"name\":\"Cleanup\",\"eventType\":\"OTHER\",\"startDateTime\":\"2027-01-01T10:00:00Z\",\"endDateTime\":\"2027-01-01T11:00:00Z\"}";
  }

  private UsernamePasswordAuthenticationToken auth() {
    return UsernamePasswordAuthenticationToken.authenticated(
        new CivicOpsPrincipal(UUID.randomUUID(), "event@example.org"), null, List.of());
  }
}
