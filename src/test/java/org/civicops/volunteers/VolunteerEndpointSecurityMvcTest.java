package org.civicops.volunteers;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;
import org.civicops.core.security.*;
import org.civicops.core.user.UserRepository;
import org.civicops.shared.web.GlobalExceptionHandler;
import org.civicops.volunteers.security.VolunteerAccessService;
import org.civicops.volunteers.volunteer.*;
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

@WebMvcTest(VolunteerController.class)
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
class VolunteerEndpointSecurityMvcTest {
  @Autowired MockMvc mvc;
  @MockitoBean VolunteerService service;
  @MockitoBean VolunteerAccessService access;
  @MockitoBean JwtService jwt;
  @MockitoBean UserRepository users;

  @Test
  void unauthenticatedVolunteerRequestReturns401() throws Exception {
    mvc.perform(get("/api/v1/organizations/{orgId}/volunteers", UUID.randomUUID()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void crossOrganizationVolunteerRequestReturns403() throws Exception {
    UUID org = UUID.randomUUID(), user = UUID.randomUUID();
    doThrow(new AccessDeniedException("cross org")).when(access).requireRead(org);
    mvc.perform(
            get("/api/v1/organizations/{orgId}/volunteers", org).with(authentication(auth(user))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
  }

  @Test
  void authorizedReadDelegatesToModulePolicy() throws Exception {
    UUID org = UUID.randomUUID(), user = UUID.randomUUID();
    when(service.list(eq(org), isNull(), isNull(), isNull(), any())).thenReturn(Page.empty());
    mvc.perform(
            get("/api/v1/organizations/{orgId}/volunteers", org).with(authentication(auth(user))))
        .andExpect(status().isOk());
    verify(access).requireRead(org);
  }

  @Test
  void unauthenticatedVolunteerUpdateReturns401() throws Exception {
    mvc.perform(
            patch(
                    "/api/v1/organizations/{orgId}/volunteers/{id}",
                    UUID.randomUUID(),
                    UUID.randomUUID())
                .contentType("application/json")
                .content("{\"firstName\":\"Updated\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void unauthorizedVolunteerUpdateReturns403() throws Exception {
    UUID org = UUID.randomUUID(), volunteer = UUID.randomUUID(), user = UUID.randomUUID();
    doThrow(new AccessDeniedException("wrong role")).when(access).requireManage(org);
    mvc.perform(
            patch("/api/v1/organizations/{orgId}/volunteers/{id}", org, volunteer)
                .with(authentication(auth(user)))
                .contentType("application/json")
                .content("{\"firstName\":\"Updated\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void authorizedVolunteerUpdateDelegatesToService() throws Exception {
    UUID org = UUID.randomUUID(), volunteer = UUID.randomUUID(), user = UUID.randomUUID();
    mvc.perform(
            patch("/api/v1/organizations/{orgId}/volunteers/{id}", org, volunteer)
                .with(authentication(auth(user)))
                .contentType("application/json")
                .content("{\"firstName\":\"Updated\"}"))
        .andExpect(status().isOk());
    verify(access).requireManage(org);
    verify(service).update(eq(org), eq(volunteer), any());
  }

  private UsernamePasswordAuthenticationToken auth(UUID id) {
    return UsernamePasswordAuthenticationToken.authenticated(
        new CivicOpsPrincipal(id, "v@example.org"), null, List.of());
  }
}
