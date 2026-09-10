package org.civicops.foodpantry;

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
import org.civicops.foodpantry.distribution.*;
import org.civicops.foodpantry.household.*;
import org.civicops.foodpantry.household.dto.*;
import org.civicops.foodpantry.inventory.*;
import org.civicops.foodpantry.item.*;
import org.civicops.foodpantry.pantry.*;
import org.civicops.foodpantry.reporting.*;
import org.civicops.foodpantry.reporting.dto.*;
import org.civicops.foodpantry.security.FoodPantryAccessService;
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
  FoodPantryLocationController.class,
  PantryItemController.class,
  PantryInventoryController.class,
  PantryHouseholdController.class,
  PantryDistributionController.class,
  FoodPantryReportingController.class
})
@Import({
  CoreSecurityConfiguration.class,
  JwtAuthenticationFilter.class,
  SecurityErrorWriter.class,
  CurrentUserProvider.class,
  GlobalExceptionHandler.class
})
@TestPropertySource(properties = "civicops.security.jwt.secret=0123456789abcdef0123456789abcdef")
class FoodPantryEndpointSecurityMvcTest {
  @Autowired MockMvc mvc;
  @MockitoBean FoodPantryLocationService locations;
  @MockitoBean PantryItemService items;
  @MockitoBean PantryInventoryService inventory;
  @MockitoBean PantryHouseholdService households;
  @MockitoBean PantryDistributionService distributions;
  @MockitoBean FoodPantryReportingService reports;
  @MockitoBean FoodPantryAccessService access;
  @MockitoBean JwtService jwt;
  @MockitoBean UserRepository users;

  @Test
  void unauthenticatedInventoryReadIs401() throws Exception {
    mvc.perform(get("/api/v1/organizations/{o}/food-pantries", UUID.randomUUID()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void managerCanCreateLocationItemReceiptHouseholdAndVisit() throws Exception {
    UUID o = UUID.randomUUID(), pantry = UUID.randomUUID(), item = UUID.randomUUID();
    when(access.userId()).thenReturn(UUID.randomUUID());
    mvc.perform(
            post("/api/v1/organizations/{o}/food-pantries", o)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"name\":\"Hope Pantry\",\"timezone\":\"UTC\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/organizations/{o}/pantry-items", o)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(
                    "{\"name\":\"Beans\",\"category\":\"CANNED_GOODS\",\"unitType\":\"CAN\",\"trackExpiration\":true}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/organizations/{o}/food-pantries/{p}/inventory-receipts", o, pantry)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(
                    "{\"itemId\":\""
                        + item
                        + "\",\"quantity\":20,\"receivedDate\":\"2030-01-01\",\"expirationDate\":\"2030-06-01\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/organizations/{o}/pantry-households", o)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"householdSize\":4}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/organizations/{o}/food-pantries/{p}/distribution-visits", o, pantry)
                .with(authentication(auth()))
                .contentType("application/json")
                .content(
                    "{\"recipientName\":\"Anonymous\",\"visitDateTime\":\"2030-01-01T12:00:00Z\",\"householdSizeAtVisit\":1}"))
        .andExpect(status().isCreated());
    verify(access, atLeast(4)).requirePantryManagement(o);
    verify(access).requireDistributionManagement(o);
  }

  @Test
  void viewerCanReadPrivacySafeInventory() throws Exception {
    UUID o = UUID.randomUUID(), p = UUID.randomUUID();
    when(inventory.list(eq(o), eq(p), any(), any(), any(), any(), any(), any()))
        .thenReturn(Page.empty());
    mvc.perform(
            get("/api/v1/organizations/{o}/food-pantries/{p}/inventory", o, p)
                .with(authentication(auth())))
        .andExpect(status().isOk());
    verify(access).requireInventoryRead(o);
  }

  @Test
  void viewerCannotReadHouseholdDetail() throws Exception {
    UUID o = UUID.randomUUID();
    doThrow(new AccessDeniedException("private")).when(access).requireHouseholdDetail(o);
    mvc.perform(
            get("/api/v1/organizations/{o}/pantry-households/{h}", o, UUID.randomUUID())
                .with(authentication(auth())))
        .andExpect(status().isForbidden());
  }

  @Test
  void householdSummaryOmitsPii() throws Exception {
    UUID o = UUID.randomUUID();
    when(households.list(eq(o), any(), any(), any(), any(), any()))
        .thenReturn(
            new PageImpl<>(
                List.of(
                    new PantryHouseholdSummaryResponse(
                        UUID.randomUUID(), "HH-1", "Citizen Household", 4, true, Instant.now()))));
    mvc.perform(get("/api/v1/organizations/{o}/pantry-households", o).with(authentication(auth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].displayName").value("Citizen Household"))
        .andExpect(jsonPath("$.content[0].email").doesNotExist())
        .andExpect(jsonPath("$.content[0].phone").doesNotExist())
        .andExpect(jsonPath("$.content[0].address").doesNotExist())
        .andExpect(jsonPath("$.content[0].notes").doesNotExist());
  }

  @Test
  void programManagerCanReadAggregateReportWithoutPii() throws Exception {
    UUID o = UUID.randomUUID();
    when(reports.distributions(eq(o), any(), any(), any()))
        .thenReturn(
            new PantryDistributionReportResponse(
                o,
                null,
                null,
                null,
                2,
                2,
                1,
                new BigDecimal("25.000"),
                new BigDecimal("3.50"),
                List.of()));
    mvc.perform(
            get("/api/v1/organizations/{o}/food-pantry-reports/distributions", o)
                .with(authentication(auth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.visitsCompleted").value(2))
        .andExpect(jsonPath("$.email").doesNotExist())
        .andExpect(jsonPath("$.householdName").doesNotExist());
    verify(access).requirePantryReporting(o);
  }

  @Test
  void programManagerCannotMutateInventory() throws Exception {
    UUID o = UUID.randomUUID();
    doThrow(new AccessDeniedException("manager only")).when(access).requirePantryManagement(o);
    mvc.perform(
            post("/api/v1/organizations/{o}/pantry-inventory/{l}/adjust", o, UUID.randomUUID())
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"adjustmentType\":\"SPOILAGE\",\"quantity\":1,\"reason\":\"Damaged\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void crossOrganizationReadIs403() throws Exception {
    UUID o = UUID.randomUUID();
    doThrow(new AccessDeniedException("cross org")).when(access).requireInventoryRead(o);
    mvc.perform(get("/api/v1/organizations/{o}/pantry-items", o).with(authentication(auth())))
        .andExpect(status().isForbidden());
  }

  @Test
  void unsupportedSortIs400() throws Exception {
    UUID o = UUID.randomUUID();
    mvc.perform(
            get("/api/v1/organizations/{o}/pantry-items?sort=notes", o)
                .with(authentication(auth())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void invalidReceiptAndHouseholdValidationReturn400() throws Exception {
    UUID o = UUID.randomUUID();
    mvc.perform(
            post(
                    "/api/v1/organizations/{o}/food-pantries/{p}/inventory-receipts",
                    o,
                    UUID.randomUUID())
                .with(authentication(auth()))
                .contentType("application/json")
                .content(
                    "{\"itemId\":\""
                        + UUID.randomUUID()
                        + "\",\"quantity\":0,\"receivedDate\":\"2030-01-01\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/v1/organizations/{o}/pantry-households", o)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("{\"householdSize\":0}"))
        .andExpect(status().isBadRequest());
  }

  private UsernamePasswordAuthenticationToken auth() {
    return UsernamePasswordAuthenticationToken.authenticated(
        new CivicOpsPrincipal(UUID.randomUUID(), "pantry@example.org"), null, List.of());
  }
}
