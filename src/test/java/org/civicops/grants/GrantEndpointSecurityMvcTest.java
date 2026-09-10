package org.civicops.grants;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;
import org.civicops.core.security.*;
import org.civicops.core.user.UserRepository;
import org.civicops.grants.expense.*;
import org.civicops.grants.grant.*;
import org.civicops.grants.reporting.*;
import org.civicops.grants.security.GrantAccessService;
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

@WebMvcTest({
  GrantController.class,
  GrantExpenseController.class,
  GrantExpenseDetailController.class,
  GrantReportingController.class
})
@Import({
  CoreSecurityConfiguration.class,
  JwtAuthenticationFilter.class,
  SecurityErrorWriter.class,
  CurrentUserProvider.class,
  GlobalExceptionHandler.class
})
@TestPropertySource(
    properties = {
      "civicops.security.jwt.secret=0123456789abcdef0123456789abcdef",
      "civicops.security.jwt.access-token-ttl=PT15M",
      "civicops.security.jwt.refresh-token-ttl=P30D"
    })
class GrantEndpointSecurityMvcTest {
  @Autowired MockMvc mvc;
  @MockitoBean GrantService grants;
  @MockitoBean GrantExpenseService expenses;
  @MockitoBean GrantReportingService reporting;
  @MockitoBean GrantAccessService access;
  @MockitoBean JwtService jwt;
  @MockitoBean UserRepository users;

  @Test
  void unauthenticatedGrantRequestReturns401() throws Exception {
    mvc.perform(get("/api/v1/organizations/{org}/grants", UUID.randomUUID()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void viewerCannotCreateGrant() throws Exception {
    UUID org = UUID.randomUUID();
    doThrow(new AccessDeniedException("read only")).when(access).requireManage(org);
    mvc.perform(
            post("/api/v1/organizations/{org}/grants", org)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(validGrant()))
        .andExpect(status().isForbidden());
  }

  @Test
  void grantManagerCanCreateGrant() throws Exception {
    UUID org = UUID.randomUUID();
    when(access.userId()).thenReturn(UUID.randomUUID());
    mvc.perform(
            post("/api/v1/organizations/{org}/grants", org)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(validGrant()))
        .andExpect(status().isCreated());
    verify(access).requireManage(org);
    verify(grants).create(eq(org), any(), any());
  }

  @Test
  void grantManagerCanRecordExpense() throws Exception {
    UUID org = UUID.randomUUID(), grant = UUID.randomUUID();
    when(access.userId()).thenReturn(UUID.randomUUID());
    mvc.perform(
            post("/api/v1/organizations/{org}/grants/{grant}/expenses", org, grant)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(
                    "{\"amount\":\"25.00\",\"expenseDate\":\"2026-08-30\",\"category\":\"SUPPLIES\",\"description\":\"Paper\"}"))
        .andExpect(status().isCreated());
    verify(expenses).create(eq(org), eq(grant), any(), any());
  }

  @Test
  void unrelatedCoordinatorCannotMutateGrant() throws Exception {
    UUID org = UUID.randomUUID(), grant = UUID.randomUUID();
    doThrow(new AccessDeniedException("wrong role")).when(access).requireManage(org);
    mvc.perform(
            post("/api/v1/organizations/{org}/grants/{grant}/activate", org, grant)
                .with(authentication(auth())))
        .andExpect(status().isForbidden());
  }

  @Test
  void crossOrganizationReadReturns403() throws Exception {
    UUID org = UUID.randomUUID();
    doThrow(new AccessDeniedException("cross org")).when(access).requireRead(org);
    mvc.perform(get("/api/v1/organizations/{org}/grants", org).with(authentication(auth())))
        .andExpect(status().isForbidden());
  }

  @Test
  void authorizedReadReturns200() throws Exception {
    UUID org = UUID.randomUUID();
    when(grants.list(
            eq(org), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
        .thenReturn(Page.empty());
    mvc.perform(get("/api/v1/organizations/{org}/grants", org).with(authentication(auth())))
        .andExpect(status().isOk());
  }

  @Test
  void unsupportedSortReturns400() throws Exception {
    mvc.perform(
            get("/api/v1/organizations/{org}/grants?sort=notes", UUID.randomUUID())
                .with(authentication(auth())))
        .andExpect(status().isBadRequest());
  }

  private UsernamePasswordAuthenticationToken auth() {
    return UsernamePasswordAuthenticationToken.authenticated(
        new CivicOpsPrincipal(UUID.randomUUID(), "grant@example.org"), null, List.of());
  }

  private String validGrant() {
    return "{\"grantName\":\"Community Grant\",\"grantorName\":\"Foundation\",\"awardAmount\":\"50000.00\"}";
  }
}
