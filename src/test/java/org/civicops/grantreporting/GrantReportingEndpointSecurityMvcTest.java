package org.civicops.grantreporting;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.civicops.core.security.*;
import org.civicops.core.user.UserRepository;
import org.civicops.grantreporting.evidence.*;
import org.civicops.grantreporting.export.GrantReportExportService;
import org.civicops.grantreporting.report.*;
import org.civicops.grantreporting.security.GrantReportingAccessService;
import org.civicops.grantreporting.template.*;
import org.civicops.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({GrantReportController.class, GrantReportTemplateController.class})
@Import({
  CoreSecurityConfiguration.class,
  JwtAuthenticationFilter.class,
  SecurityErrorWriter.class,
  CurrentUserProvider.class,
  GlobalExceptionHandler.class
})
@TestPropertySource(properties = "civicops.security.jwt.secret=0123456789abcdef0123456789abcdef")
class GrantReportingEndpointSecurityMvcTest {
  @Autowired MockMvc mvc;
  @MockitoBean GrantReportService reports;
  @MockitoBean GrantReportTemplateService templates;
  @MockitoBean GrantReportExportService exports;
  @MockitoBean GrantReportingAccessService access;
  @MockitoBean JwtService jwt;
  @MockitoBean UserRepository users;

  @Test
  void unauthenticatedRouteIs401() throws Exception {
    mvc.perform(get("/api/v1/organizations/{o}/grant-reports", UUID.randomUUID()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void grantManagerCanManageCompleteWorkflow() throws Exception {
    UUID o = UUID.randomUUID(), grant = UUID.randomUUID(), template = UUID.randomUUID();
    when(access.userId()).thenReturn(UUID.randomUUID());
    mvc.perform(
            post("/api/v1/organizations/{o}/grant-report-templates", o)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"name\":\"Foundation Report\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/organizations/{o}/grant-report-templates/{t}/sections", o, template)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(
                    "{\"sectionKey\":\"summary\",\"title\":\"Summary\",\"sequenceNumber\":1,\"sectionType\":\"NARRATIVE\",\"required\":true}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/organizations/{o}/grants/{g}/reports", o, grant)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(
                    "{\"templateId\":\""
                        + template
                        + "\",\"reportingPeriodStart\":\"2026-01-01\",\"reportingPeriodEnd\":\"2026-06-30\",\"selectedSources\":[\"GRANT\"]}"))
        .andExpect(status().isCreated());
    UUID report = UUID.randomUUID(), section = UUID.randomUUID();
    mvc.perform(
            post("/api/v1/organizations/{o}/grant-reports/{r}/generate", o, report)
                .with(authentication(auth())))
        .andExpect(status().isOk());
    mvc.perform(
            patch("/api/v1/organizations/{o}/grant-report-sections/{s}", o, section)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"editedContent\":\"Reviewed factual narrative.\"}"))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/v1/organizations/{o}/grant-report-sections/{s}/approve", o, section)
                .with(authentication(auth())))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/v1/organizations/{o}/grant-reports/{r}/finalize", o, report)
                .with(authentication(auth())))
        .andExpect(status().isOk());
    verify(access, atLeast(4)).requireReportManagement(o);
    verify(access, times(2)).requireTemplateManagement(o);
    verify(access).requireFinalize(o);
  }

  @Test
  void programManagerCanReadButServiceRestrictsReadsToFinalizedReports() throws Exception {
    UUID o = UUID.randomUUID();
    when(access.canManage(o)).thenReturn(false);
    when(reports.list(eq(o), isNull(), isNull(), eq(false), any())).thenReturn(Page.empty());
    mvc.perform(get("/api/v1/organizations/{o}/grant-reports", o).with(authentication(auth())))
        .andExpect(status().isOk());
    verify(access).requireReportRead(o);
    verify(reports).list(eq(o), isNull(), isNull(), eq(false), any());
  }

  @Test
  void viewerAndUnrelatedRolesCannotManageDrafts() throws Exception {
    UUID o = UUID.randomUUID();
    doThrow(new AccessDeniedException("grant manager required"))
        .when(access)
        .requireReportManagement(o);
    mvc.perform(
            post("/api/v1/organizations/{o}/grant-reports/{r}/generate", o, UUID.randomUUID())
                .with(authentication(auth())))
        .andExpect(status().isForbidden());
  }

  @Test
  void crossOrganizationReadIs403() throws Exception {
    UUID o = UUID.randomUUID();
    doThrow(new AccessDeniedException("cross organization")).when(access).requireReportRead(o);
    mvc.perform(
            get("/api/v1/organizations/{o}/grant-reports/{r}", o, UUID.randomUUID())
                .with(authentication(auth())))
        .andExpect(status().isForbidden());
  }

  @Test
  void evidenceJsonIsAggregateAndContainsNoSensitiveIdentityFields() throws Exception {
    UUID o = UUID.randomUUID(), report = UUID.randomUUID();
    var aggregate =
        new EvidenceDtos.Response(
            UUID.randomUUID(),
            EvidenceSourceModule.CASES,
            "CaseReportingSummary",
            null,
            "case.services_provided",
            "Services provided",
            EvidenceValueState.VERIFIED,
            new BigDecimal("12"),
            null,
            null,
            "services",
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 6, 30),
            Instant.parse("2026-07-01T00:00:00Z"),
            "Aggregate Case reporting service",
            null,
            null);
    when(access.canManage(o)).thenReturn(true);
    when(reports.evidence(o, report, true)).thenReturn(List.of(aggregate));
    mvc.perform(
            get("/api/v1/organizations/{o}/grant-reports/{r}/evidence", o, report)
                .with(authentication(auth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].metricKey").value("case.services_provided"))
        .andExpect(jsonPath("$[0].numericValue").value(12))
        .andExpect(jsonPath("$[0].clientName").doesNotExist())
        .andExpect(jsonPath("$[0].caseNotes").doesNotExist())
        .andExpect(jsonPath("$[0].householdName").doesNotExist())
        .andExpect(jsonPath("$[0].applicantName").doesNotExist())
        .andExpect(jsonPath("$[0].donorEmail").doesNotExist())
        .andExpect(jsonPath("$[0].attendeePhone").doesNotExist());
  }

  @Test
  void invalidPeriodManualEvidenceAndTemplateSectionReturn400() throws Exception {
    UUID o = UUID.randomUUID(),
        grant = UUID.randomUUID(),
        template = UUID.randomUUID(),
        report = UUID.randomUUID();
    mvc.perform(
            post("/api/v1/organizations/{o}/grants/{g}/reports", o, grant)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"reportingPeriodStart\":\"2026-01-01\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/v1/organizations/{o}/grant-reports/{r}/manual-evidence", o, report)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"label\":\"Manual value\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/v1/organizations/{o}/grant-report-templates/{t}/sections", o, template)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"sectionKey\":\"Bad Key\",\"title\":\"Title\",\"sequenceNumber\":0}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void unsupportedSortAndExportFormatReturnClientErrors() throws Exception {
    UUID o = UUID.randomUUID(), report = UUID.randomUUID();
    mvc.perform(
            get("/api/v1/organizations/{o}/grant-reports?sort=grantName", o)
                .with(authentication(auth())))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/v1/organizations/{o}/grant-reports/{r}/export?format=pdf", o, report)
                .with(authentication(auth())))
        .andExpect(status().isUnprocessableEntity());
  }

  private UsernamePasswordAuthenticationToken auth() {
    return UsernamePasswordAuthenticationToken.authenticated(
        new CivicOpsPrincipal(UUID.randomUUID(), "grant.manager@example.org"), null, List.of());
  }
}
