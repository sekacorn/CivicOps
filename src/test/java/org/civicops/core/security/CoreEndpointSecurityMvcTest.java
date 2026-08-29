package org.civicops.core.security;

import org.civicops.core.membership.OrganizationMembershipController;
import org.civicops.core.membership.OrganizationMembershipService;
import org.civicops.core.membership.Role;
import org.civicops.core.user.User;
import org.civicops.core.user.UserRepository;
import org.civicops.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizationMembershipController.class)
@Import({CoreSecurityConfiguration.class, JwtAuthenticationFilter.class, SecurityErrorWriter.class,
        CurrentUserProvider.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "civicops.security.jwt.secret=0123456789abcdef0123456789abcdef",
        "civicops.security.jwt.access-token-ttl=PT15M",
        "civicops.security.jwt.refresh-token-ttl=P30D"
})
class CoreEndpointSecurityMvcTest {
    @Autowired MockMvc mvc;
    @MockitoBean OrganizationMembershipService membershipService;
    @MockitoBean OrganizationAccessService access;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserRepository users;

    @Test
    void missingTokenReturns401() throws Exception {
        mvc.perform(get("/api/v1/organizations/{id}/memberships", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        when(jwtService.decode("invalid-token")).thenThrow(new BadJwtException("invalid"));
        mvc.perform(get("/api/v1/organizations/{id}/memberships", UUID.randomUUID())
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validAccessTokenAuthenticatesAndAuthorizesRequest() throws Exception {
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Jwt jwt = mock(Jwt.class);
        User user = mock(User.class);
        when(jwt.getSubject()).thenReturn(userId.toString());
        when(jwtService.decode("valid-token")).thenReturn(jwt);
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(user.isActive()).thenReturn(true);
        when(user.getEmail()).thenReturn("jane@example.org");
        when(membershipService.listForOrganization(organizationId)).thenReturn(List.of());

        mvc.perform(get("/api/v1/organizations/{id}/memberships", organizationId)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());

        verify(access).requireRole(userId, organizationId, Role.ORG_ADMIN);
    }

    @Test
    void authenticatedWrongRoleReturns403() throws Exception {
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        doThrow(new AccessDeniedException("wrong role"))
                .when(access).requireRole(userId, organizationId, Role.ORG_ADMIN);

        mvc.perform(get("/api/v1/organizations/{id}/memberships", organizationId)
                        .with(authentication(authToken(userId))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void crossOrganizationRequestReturns403() throws Exception {
        UUID organizationB = UUID.randomUUID();
        UUID userFromOrganizationA = UUID.randomUUID();
        doThrow(new AccessDeniedException("no membership"))
                .when(access).requireRole(userFromOrganizationA, organizationB, Role.ORG_ADMIN);

        mvc.perform(get("/api/v1/organizations/{id}/memberships", organizationB)
                        .with(authentication(authToken(userFromOrganizationA))))
                .andExpect(status().isForbidden());
    }

    private UsernamePasswordAuthenticationToken authToken(UUID userId) {
        return UsernamePasswordAuthenticationToken.authenticated(
                new CivicOpsPrincipal(userId, "user@example.org"), null, List.of());
    }
}
