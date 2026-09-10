package org.civicops.donations;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.*;
import org.civicops.core.security.*;
import org.civicops.core.user.UserRepository;
import org.civicops.donations.campaign.*;
import org.civicops.donations.donation.*;
import org.civicops.donations.donor.*;
import org.civicops.donations.reporting.*;
import org.civicops.donations.reporting.dto.DonationReportSummaryResponse;
import org.civicops.donations.security.DonationAccessService;
import org.civicops.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({
  DonorController.class,
  DonationController.class,
  DonationCampaignController.class,
  DonationReportingController.class
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
class DonationEndpointSecurityMvcTest {
  @Autowired MockMvc mvc;
  @MockitoBean DonorService donors;
  @MockitoBean DonationService donations;
  @MockitoBean DonationCampaignService campaigns;
  @MockitoBean DonationReportingService reporting;
  @MockitoBean DonationAccessService access;
  @MockitoBean JwtService jwt;
  @MockitoBean UserRepository users;

  @Test
  void unauthenticatedReturns401() throws Exception {
    mvc.perform(get("/api/v1/organizations/{org}/donations", UUID.randomUUID()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void donationManagerCreatesDonor() throws Exception {
    UUID org = UUID.randomUUID();
    mvc.perform(
            post("/api/v1/organizations/{org}/donors", org)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(
                    "{\"donorType\":\"INDIVIDUAL\",\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}"))
        .andExpect(status().isCreated());
    verify(access).requireManage(org);
  }

  @Test
  void donationManagerRecordsDonation() throws Exception {
    UUID org = UUID.randomUUID();
    when(access.userId()).thenReturn(UUID.randomUUID());
    mvc.perform(
            post("/api/v1/organizations/{org}/donations", org)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(
                    "{\"anonymous\":true,\"amount\":10.00,\"donationDate\":\"2026-08-30\",\"paymentMethod\":\"CASH\"}"))
        .andExpect(status().isCreated());
    verify(donations).create(eq(org), any(), any());
  }

  @Test
  void viewerCannotCreateDonation() throws Exception {
    UUID org = UUID.randomUUID();
    doThrow(new AccessDeniedException("read only")).when(access).requireManage(org);
    mvc.perform(
            post("/api/v1/organizations/{org}/donations", org)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(
                    "{\"anonymous\":true,\"amount\":10,\"donationDate\":\"2026-08-30\",\"paymentMethod\":\"CASH\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void programManagerCannotPatchPrivateDonor() throws Exception {
    UUID org = UUID.randomUUID();
    doThrow(new AccessDeniedException("read only")).when(access).requireManage(org);
    mvc.perform(
            patch("/api/v1/organizations/{org}/donors/{id}", org, UUID.randomUUID())
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"phone\":\"555-0100\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void volunteerCannotReadDonations() throws Exception {
    UUID org = UUID.randomUUID();
    doThrow(new AccessDeniedException("wrong role")).when(access).requireSummaryRead(org);
    mvc.perform(get("/api/v1/organizations/{org}/donations", org).with(authentication(auth())))
        .andExpect(status().isForbidden());
  }

  @Test
  void crossOrganizationAccessReturns403() throws Exception {
    UUID org = UUID.randomUUID();
    doThrow(new AccessDeniedException("cross org")).when(access).requireSummaryRead(org);
    mvc.perform(
            get("/api/v1/organizations/{org}/donation-reports/summary", org)
                .with(authentication(auth())))
        .andExpect(status().isForbidden());
  }

  @Test
  void authorizedSummaryReturns200() throws Exception {
    UUID org = UUID.randomUUID();
    when(reporting.summary(org, null, null))
        .thenReturn(
            new DonationReportSummaryResponse(
                0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0));
    mvc.perform(
            get("/api/v1/organizations/{org}/donation-reports/summary", org)
                .with(authentication(auth())))
        .andExpect(status().isOk());
  }

  @Test
  void unsupportedSortReturns400() throws Exception {
    mvc.perform(
            get("/api/v1/organizations/{org}/donations?sort=notes", UUID.randomUUID())
                .with(authentication(auth())))
        .andExpect(status().isBadRequest());
  }

  private UsernamePasswordAuthenticationToken auth() {
    return UsernamePasswordAuthenticationToken.authenticated(
        new CivicOpsPrincipal(UUID.randomUUID(), "donation@example.org"), null, List.of());
  }
}
