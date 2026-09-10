package org.civicops.foodpantry;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.*;
import java.time.*;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.foodpantry.distribution.*;
import org.civicops.foodpantry.household.*;
import org.civicops.foodpantry.household.dto.PantryHouseholdSummaryResponse;
import org.civicops.foodpantry.inventory.*;
import org.civicops.foodpantry.item.*;
import org.civicops.foodpantry.pantry.*;
import org.civicops.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class FoodPantryDomainTest {
  Organization org = mock(Organization.class);
  User user = mock(User.class);

  @Test
  void locationValidatesIanaTimezoneAndPreservesDeactivation() {
    var p =
        new FoodPantryLocation(
            org, "Hope", null, null, null, null, null, null, "US", "America/New_York", null);
    p.deactivate();
    assertThat(p.isActive()).isFalse();
    assertThatThrownBy(
            () ->
                new FoodPantryLocation(
                    org, "Bad", null, null, null, null, null, null, null, "Not/AZone", null))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void itemNormalizesSkuAndValidatesThreshold() {
    var i = item(true);
    assertThat(i.getNormalizedSku()).isEqualTo("BEANS-1");
    assertThatThrownBy(
            () ->
                new PantryItem(
                    org,
                    null,
                    "Bad",
                    null,
                    FoodCategory.OTHER,
                    UnitType.EACH,
                    false,
                    new BigDecimal("-1"),
                    null))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void trackedItemRequiresExpiration() {
    assertThatThrownBy(() -> lot(item(true), null, BigDecimal.ONE))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void expirationCannotPrecedeReceipt() {
    assertThatThrownBy(
            () ->
                new PantryInventoryLot(
                    pantry(),
                    item(false),
                    null,
                    LocalDate.now(),
                    LocalDate.now().minusDays(1),
                    BigDecimal.ONE,
                    null,
                    null,
                    null,
                    user))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void lotNeverConsumesBelowZero() {
    var l = lot(item(false), null, new BigDecimal("10"));
    l.consume(new BigDecimal("7"));
    assertThat(l.getQuantityRemaining()).isEqualByComparingTo("3.000");
    assertThatThrownBy(() -> l.consume(new BigDecimal("4")))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void countIncreaseCannotExceedOriginalReceipt() {
    var l = lot(item(false), null, new BigDecimal("5"));
    l.decrease(new BigDecimal("2"));
    l.increase(BigDecimal.ONE);
    assertThat(l.getQuantityRemaining()).isEqualByComparingTo("4.000");
    assertThatThrownBy(() -> l.increase(new BigDecimal("2")))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void expirationBoundaryIsInclusive() {
    var today = LocalDate.now();
    assertThat(lot(item(true), today, BigDecimal.ONE).isExpired(today)).isFalse();
    assertThat(lot(item(true), today.minusDays(1), BigDecimal.ONE).isExpired(today)).isTrue();
  }

  @Test
  void householdMinimizesPiiInSummary() {
    var h =
        new PantryHousehold(
            org,
            "HH-1",
            "Citizen Household",
            "Ada",
            "Lovelace",
            "private@example.org",
            "555",
            "Private address",
            4,
            "Private notes");
    assertThat(PantryHouseholdSummaryResponse.from(h).toString())
        .doesNotContain("private@example.org", "Private address", "Private notes", "555");
  }

  @Test
  void householdSizeMustBePositive() {
    assertThatThrownBy(
            () -> new PantryHousehold(org, null, null, null, null, null, null, null, 0, null))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void cancelledVisitIsTerminalAndDoesNotAllocate() {
    var v = new PantryDistributionVisit(pantry(), null, "Anonymous", Instant.now(), 1, null, user);
    v.cancel(Instant.now());
    assertThat(v.getStatus()).isEqualTo(DistributionVisitStatus.CANCELLED);
    assertThatThrownBy(() -> v.complete(Instant.now())).isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void distributionQuantityMustBePositive() {
    var v = new PantryDistributionVisit(pantry(), null, "Anonymous", Instant.now(), 1, null, user);
    assertThatThrownBy(() -> new PantryDistributionItem(v, item(false), BigDecimal.ZERO))
        .isInstanceOf(BusinessRuleException.class);
  }

  private FoodPantryLocation pantry() {
    return new FoodPantryLocation(
        org, "Hope", null, null, null, null, null, null, null, "UTC", null);
  }

  private PantryItem item(boolean expiration) {
    return new PantryItem(
        org,
        " beans-1 ",
        "Beans",
        null,
        FoodCategory.CANNED_GOODS,
        UnitType.CAN,
        expiration,
        BigDecimal.ZERO,
        null);
  }

  private PantryInventoryLot lot(PantryItem item, LocalDate expiration, BigDecimal q) {
    return new PantryInventoryLot(
        pantry(), item, null, LocalDate.now().minusDays(1), expiration, q, null, null, null, user);
  }
}
