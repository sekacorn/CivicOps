package org.civicops.volunteers;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.*;
import org.civicops.core.security.*;
import org.civicops.core.user.UserRepository;
import org.civicops.shared.web.GlobalExceptionHandler;
import org.civicops.volunteers.opportunity.*;
import org.civicops.volunteers.security.VolunteerAccessService;
import org.civicops.volunteers.shift.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({OpportunityController.class, ShiftController.class})
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
class VolunteerUpdateEndpointSecurityMvcTest {
  @Autowired MockMvc mvc;
  @MockitoBean OpportunityService opportunities;
  @MockitoBean ShiftService shifts;
  @MockitoBean VolunteerAccessService access;
  @MockitoBean JwtService jwt;
  @MockitoBean UserRepository users;

  @Test
  void unauthenticatedShiftUpdateReturns401() throws Exception {
    mvc.perform(
            patch(
                    "/api/v1/organizations/{org}/volunteer-opportunities/{opp}/shifts/{shift}",
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID())
                .contentType("application/json")
                .content("{\"capacity\":8}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void forbiddenOpportunityUpdateReturns403() throws Exception {
    UUID org = UUID.randomUUID();
    doThrow(new AccessDeniedException("viewer")).when(access).requireOpportunityManage(org);
    mvc.perform(
            patch(
                    "/api/v1/organizations/{org}/volunteer-opportunities/{opp}",
                    org,
                    UUID.randomUUID())
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"title\":\"Updated\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void authorizedShiftUpdateDelegatesToService() throws Exception {
    UUID org = UUID.randomUUID(), opp = UUID.randomUUID(), shift = UUID.randomUUID();
    mvc.perform(
            patch(
                    "/api/v1/organizations/{org}/volunteer-opportunities/{opp}/shifts/{shift}",
                    org,
                    opp,
                    shift)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"capacity\":8}"))
        .andExpect(status().isOk());
    verify(access).requireOpportunityManage(org);
    verify(shifts).update(eq(org), eq(opp), eq(shift), any());
  }

  private UsernamePasswordAuthenticationToken auth() {
    return UsernamePasswordAuthenticationToken.authenticated(
        new CivicOpsPrincipal(UUID.randomUUID(), "manager@example.org"), null, List.of());
  }
}
