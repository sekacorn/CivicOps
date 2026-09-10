package org.civicops.equipment;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;
import org.civicops.core.security.*;
import org.civicops.core.user.UserRepository;
import org.civicops.equipment.asset.*;
import org.civicops.equipment.asset.dto.*;
import org.civicops.equipment.category.*;
import org.civicops.equipment.checkout.*;
import org.civicops.equipment.maintenance.*;
import org.civicops.equipment.reporting.*;
import org.civicops.equipment.security.EquipmentAccessService;
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
  EquipmentCategoryController.class,
  EquipmentAssetController.class,
  EquipmentCheckoutController.class,
  EquipmentMaintenanceController.class,
  EquipmentReportingController.class
})
@Import({
  CoreSecurityConfiguration.class,
  JwtAuthenticationFilter.class,
  SecurityErrorWriter.class,
  CurrentUserProvider.class,
  GlobalExceptionHandler.class
})
@TestPropertySource(properties = "civicops.security.jwt.secret=0123456789abcdef0123456789abcdef")
class EquipmentEndpointSecurityMvcTest {
  @Autowired MockMvc mvc;
  @MockitoBean EquipmentCategoryService categories;
  @MockitoBean EquipmentAssetService assets;
  @MockitoBean EquipmentCheckoutService checkouts;
  @MockitoBean EquipmentMaintenanceService maintenance;
  @MockitoBean EquipmentReportingService reports;
  @MockitoBean EquipmentAccessService access;
  @MockitoBean JwtService jwt;
  @MockitoBean UserRepository users;

  @Test
  void unauthenticatedInventoryReturns401() throws Exception {
    mvc.perform(get("/api/v1/organizations/{org}/equipment", UUID.randomUUID()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void equipmentManagerCanCreateAsset() throws Exception {
    UUID org = UUID.randomUUID();
    mvc.perform(
            post("/api/v1/organizations/{org}/equipment", org)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"assetTag\":\"LAP-1\",\"name\":\"Laptop\",\"condition\":\"GOOD\"}"))
        .andExpect(status().isCreated());
    verify(access).requireManageEquipment(org);
  }

  @Test
  void viewerCanReadPrivacySafeInventoryWithoutBorrowerData() throws Exception {
    UUID org = UUID.randomUUID();
    when(assets.list(eq(org), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(
            new PageImpl<>(
                List.of(
                    new EquipmentAssetSummaryResponse(
                        UUID.randomUUID(),
                        "LAP-1",
                        "Laptop",
                        null,
                        null,
                        EquipmentCondition.GOOD,
                        AssetStatus.CHECKED_OUT,
                        "Office"))));
    mvc.perform(get("/api/v1/organizations/{org}/equipment", org).with(authentication(auth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].assetTag").value("LAP-1"))
        .andExpect(jsonPath("$.content[0].borrowerEmail").doesNotExist())
        .andExpect(jsonPath("$.content[0].borrowerName").doesNotExist())
        .andExpect(jsonPath("$.content[0].notes").doesNotExist());
    verify(access).requireReadEquipment(org);
  }

  @Test
  void viewerMutationDenialIsReturnedAs403() throws Exception {
    UUID org = UUID.randomUUID();
    doThrow(new AccessDeniedException("read only")).when(access).requireManageEquipment(org);
    mvc.perform(
            post("/api/v1/organizations/{org}/equipment", org)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"assetTag\":\"LAP-1\",\"name\":\"Laptop\",\"condition\":\"GOOD\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void managerCanCheckoutCheckInAndManageMaintenance() throws Exception {
    UUID org = UUID.randomUUID(),
        asset = UUID.randomUUID(),
        checkout = UUID.randomUUID(),
        record = UUID.randomUUID();
    when(access.userId()).thenReturn(UUID.randomUUID());
    mvc.perform(
            post("/api/v1/organizations/{org}/equipment/{asset}/checkouts", org, asset)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(
                    "{\"borrowerName\":\"External Borrower\",\"dueAt\":\"2030-01-01T12:00:00Z\",\"checkoutCondition\":\"GOOD\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/organizations/{org}/equipment-checkouts/{id}/check-in", org, checkout)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"returnCondition\":\"GOOD\"}"))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/v1/organizations/{org}/equipment/{asset}/maintenance", org, asset)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"maintenanceType\":\"INSPECTION\",\"description\":\"Inspect\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/organizations/{org}/equipment-maintenance/{id}/start", org, record)
                .with(authentication(auth())))
        .andExpect(status().isOk());
    verify(access, times(2)).requireCheckoutManagement(org);
    verify(access, times(2)).requireMaintenanceManagement(org);
  }

  @Test
  void volunteerAdministrativeMutationReturns403() throws Exception {
    UUID org = UUID.randomUUID();
    doThrow(new AccessDeniedException("no equipment role"))
        .when(access)
        .requireCheckoutManagement(org);
    mvc.perform(
            post("/api/v1/organizations/{org}/equipment/{asset}/checkouts", org, UUID.randomUUID())
                .with(authentication(auth()))
                .contentType("application/json")
                .content(
                    "{\"borrowerName\":\"Borrower\",\"dueAt\":\"2030-01-01T12:00:00Z\",\"checkoutCondition\":\"GOOD\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void crossOrganizationInventoryAccessReturns403() throws Exception {
    UUID org = UUID.randomUUID();
    doThrow(new AccessDeniedException("cross org")).when(access).requireReadEquipment(org);
    mvc.perform(get("/api/v1/organizations/{org}/equipment", org).with(authentication(auth())))
        .andExpect(status().isForbidden());
  }

  @Test
  void unsupportedEquipmentCheckoutAndMaintenanceSortsReturn400() throws Exception {
    UUID org = UUID.randomUUID();
    mvc.perform(
            get("/api/v1/organizations/{org}/equipment?sort=notes", org)
                .with(authentication(auth())))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/v1/organizations/{org}/equipment-checkouts?sort=borrowerEmail", org)
                .with(authentication(auth())))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/v1/organizations/{org}/equipment-maintenance?sort=cost", org)
                .with(authentication(auth())))
        .andExpect(status().isBadRequest());
  }

  private UsernamePasswordAuthenticationToken auth() {
    return UsernamePasswordAuthenticationToken.authenticated(
        new CivicOpsPrincipal(UUID.randomUUID(), "equipment@example.org"), null, List.of());
  }
}
