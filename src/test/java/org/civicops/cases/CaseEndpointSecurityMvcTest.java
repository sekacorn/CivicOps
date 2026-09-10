package org.civicops.cases;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.*;
import java.util.*;
import org.civicops.cases.casefile.*;
import org.civicops.cases.casefile.dto.*;
import org.civicops.cases.client.*;
import org.civicops.cases.client.dto.*;
import org.civicops.cases.note.*;
import org.civicops.cases.reporting.*;
import org.civicops.cases.security.CaseAccessService;
import org.civicops.cases.service.*;
import org.civicops.cases.task.*;
import org.civicops.core.security.*;
import org.civicops.core.user.UserRepository;
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
  ClientController.class,
  CaseRecordController.class,
  CaseNoteController.class,
  CaseTaskController.class,
  CaseServiceRecordController.class,
  CaseReportingController.class
})
@Import({
  CoreSecurityConfiguration.class,
  JwtAuthenticationFilter.class,
  SecurityErrorWriter.class,
  CurrentUserProvider.class,
  GlobalExceptionHandler.class
})
@TestPropertySource(properties = "civicops.security.jwt.secret=0123456789abcdef0123456789abcdef")
class CaseEndpointSecurityMvcTest {
  @Autowired MockMvc mvc;
  @MockitoBean ClientService clients;
  @MockitoBean CaseRecordService cases;
  @MockitoBean CaseNoteService notes;
  @MockitoBean CaseTaskService tasks;
  @MockitoBean CaseServiceRecordService services;
  @MockitoBean CaseReportingService reports;
  @MockitoBean CaseAccessService access;
  @MockitoBean JwtService jwt;
  @MockitoBean UserRepository users;

  @Test
  void unauthenticatedCaseReadReturns401() throws Exception {
    mvc.perform(get("/api/v1/organizations/{org}/cases", UUID.randomUUID()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void caseManagerCanCreateAndAssign() throws Exception {
    UUID org = UUID.randomUUID(),
        client = UUID.randomUUID(),
        worker = UUID.randomUUID(),
        id = UUID.randomUUID();
    when(access.userId()).thenReturn(UUID.randomUUID());
    mvc.perform(
            post("/api/v1/organizations/{org}/cases", org)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(
                    "{\"clientId\":\""
                        + client
                        + "\",\"caseNumber\":\"CASE-1\",\"title\":\"Support\",\"caseType\":\"GENERAL_ASSISTANCE\",\"priority\":\"NORMAL\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/organizations/{org}/cases/{id}/assign", org, id)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"userId\":\"" + worker + "\"}"))
        .andExpect(status().isOk());
    verify(access, times(2)).requireCaseManager(org);
  }

  @Test
  void workerCannotArbitrarilyReassign() throws Exception {
    UUID org = UUID.randomUUID();
    doThrow(new AccessDeniedException("manager only")).when(access).requireCaseManager(org);
    mvc.perform(
            post("/api/v1/organizations/{org}/cases/{id}/assign", org, UUID.randomUUID())
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"userId\":\"" + UUID.randomUUID() + "\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void unrelatedWorkerCaseDetailReturns403() throws Exception {
    UUID org = UUID.randomUUID(), id = UUID.randomUUID();
    CaseRecord c = mock(CaseRecord.class);
    when(cases.require(org, id)).thenReturn(c);
    doThrow(new AccessDeniedException("not assigned"))
        .when(access)
        .requireSensitiveCaseAccess(org, c);
    mvc.perform(get("/api/v1/organizations/{org}/cases/{id}", org, id).with(authentication(auth())))
        .andExpect(status().isForbidden());
  }

  @Test
  void authorizedDetailMapsInsideTransactionalServiceBoundary() throws Exception {
    UUID org = UUID.randomUUID(), id = UUID.randomUUID();
    CaseRecord c = mock(CaseRecord.class);
    when(cases.require(org, id)).thenReturn(c);
    mvc.perform(get("/api/v1/organizations/{org}/cases/{id}", org, id).with(authentication(auth())))
        .andExpect(status().isOk());
    verify(access).requireSensitiveCaseAccess(org, c);
    verify(cases).detail(org, id);
  }

  @Test
  void assignedWorkerCanCreateNoteTaskAndService() throws Exception {
    UUID org = UUID.randomUUID(), id = UUID.randomUUID();
    when(cases.require(org, id)).thenReturn(mock(CaseRecord.class));
    when(access.userId()).thenReturn(UUID.randomUUID());
    mvc.perform(
            post("/api/v1/organizations/{org}/cases/{id}/notes", org, id)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"noteType\":\"PROGRESS\",\"content\":\"Client contacted\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/organizations/{org}/cases/{id}/tasks", org, id)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"title\":\"Follow up\",\"priority\":\"NORMAL\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/organizations/{org}/cases/{id}/services", org, id)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"serviceType\":\"OTHER\",\"serviceDate\":\"2026-08-31\"}"))
        .andExpect(status().isCreated());
    verify(access, times(3)).requireAssignedCaseAccess(eq(org), any());
  }

  @Test
  void programManagerCanReadAggregateButNotWorkloadWhenPolicyDenies() throws Exception {
    UUID org = UUID.randomUUID();
    mvc.perform(
            get("/api/v1/organizations/{org}/case-reports/summary", org)
                .with(authentication(auth())))
        .andExpect(status().isOk());
    verify(access).requireReporting(org);
    doThrow(new AccessDeniedException("manager only")).when(access).requireWorkloadReporting(org);
    mvc.perform(
            get("/api/v1/organizations/{org}/case-reports/workload", org)
                .with(authentication(auth())))
        .andExpect(status().isForbidden());
  }

  @Test
  void clientSummaryJsonProtectsPii() throws Exception {
    UUID org = UUID.randomUUID();
    when(clients.list(eq(org), any(), any(), any(), any(), any()))
        .thenReturn(
            new PageImpl<>(
                List.of(
                    new ClientSummaryResponse(
                        UUID.randomUUID(), "Ada Lovelace", true, "EXT-1", Instant.now()))));
    mvc.perform(get("/api/v1/organizations/{org}/clients", org).with(authentication(auth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].displayName").value("Ada Lovelace"))
        .andExpect(jsonPath("$.content[0].dateOfBirth").doesNotExist())
        .andExpect(jsonPath("$.content[0].email").doesNotExist())
        .andExpect(jsonPath("$.content[0].phone").doesNotExist())
        .andExpect(jsonPath("$.content[0].addressLine1").doesNotExist());
  }

  @Test
  void caseSummaryJsonProtectsClientAndHistoryDetails() throws Exception {
    UUID org = UUID.randomUUID();
    when(cases.list(eq(org), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(
            new PageImpl<>(
                List.of(
                    new CaseSummaryResponse(
                        UUID.randomUUID(),
                        "CASE-1",
                        "Support",
                        CaseType.HOUSING,
                        CasePriority.HIGH,
                        CaseStatus.OPEN,
                        LocalDate.now(),
                        null,
                        UUID.randomUUID(),
                        "Ada",
                        null,
                        Instant.now()))));
    mvc.perform(get("/api/v1/organizations/{org}/cases", org).with(authentication(auth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].caseNumber").value("CASE-1"))
        .andExpect(jsonPath("$.content[0].notes").doesNotExist())
        .andExpect(jsonPath("$.content[0].clientEmail").doesNotExist())
        .andExpect(jsonPath("$.content[0].dateOfBirth").doesNotExist())
        .andExpect(jsonPath("$.content[0].services").doesNotExist());
  }

  @Test
  void unsafeClientAndCaseSortsReturn400() throws Exception {
    mvc.perform(
            get("/api/v1/organizations/{org}/clients?sort=email", UUID.randomUUID())
                .with(authentication(auth())))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/v1/organizations/{org}/cases?sort=description", UUID.randomUUID())
                .with(authentication(auth())))
        .andExpect(status().isBadRequest());
  }

  private UsernamePasswordAuthenticationToken auth() {
    return UsernamePasswordAuthenticationToken.authenticated(
        new CivicOpsPrincipal(UUID.randomUUID(), "case@example.org"), null, List.of());
  }
}
