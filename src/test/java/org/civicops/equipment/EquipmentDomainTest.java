package org.civicops.equipment;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.time.*;
import org.civicops.core.organization.Organization;
import org.civicops.core.user.User;
import org.civicops.equipment.asset.*;
import org.civicops.equipment.checkout.*;
import org.civicops.equipment.maintenance.*;
import org.civicops.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class EquipmentDomainTest {
  private final Organization org = mock(Organization.class);
  private final User user = mock(User.class);

  private EquipmentAsset asset() {
    return new EquipmentAsset(
        org,
        "LAP-1",
        "Laptop",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        EquipmentCondition.GOOD,
        null,
        null);
  }

  @Test
  void checkoutAndGoodReturnRestoreAvailability() {
    EquipmentAsset a = asset();
    a.checkout();
    assertThat(a.getStatus()).isEqualTo(AssetStatus.CHECKED_OUT);
    a.checkIn(EquipmentCondition.GOOD);
    assertThat(a.getStatus()).isEqualTo(AssetStatus.AVAILABLE);
  }

  @Test
  void damagedReturnRequiresMaintenance() {
    EquipmentAsset a = asset();
    a.checkout();
    a.checkIn(EquipmentCondition.DAMAGED);
    assertThat(a.getCondition()).isEqualTo(EquipmentCondition.DAMAGED);
    assertThat(a.getStatus()).isEqualTo(AssetStatus.MAINTENANCE);
    a.completeMaintenance(EquipmentCondition.GOOD);
    assertThat(a.getStatus()).isEqualTo(AssetStatus.AVAILABLE);
  }

  @Test
  void unavailableAndRetiredAssetsRejectCheckout() {
    EquipmentAsset a = asset();
    a.beginMaintenance();
    assertThatThrownBy(a::checkout).isInstanceOf(BusinessRuleException.class);
    a.completeMaintenance(EquipmentCondition.GOOD);
    a.retire();
    assertThatThrownBy(a::checkout).isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(
            () -> a.update("Changed", null, null, null, null, null, null, null, null, null, null))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void activeCheckoutDerivesOverdueWithoutPersistedStaleStatus() {
    EquipmentAsset a = asset();
    Instant out = Instant.parse("2026-01-01T10:00:00Z"), due = out.plusSeconds(3600);
    EquipmentCheckout c =
        new EquipmentCheckout(
            org, a, null, "External", null, out, due, EquipmentCondition.GOOD, null, user);
    assertThat(c.isOverdue(due.minusSeconds(1))).isFalse();
    assertThat(c.isOverdue(due.plusSeconds(1))).isTrue();
    c.checkIn(due.plusSeconds(2), EquipmentCondition.GOOD, user, null);
    assertThat(c.isOverdue(due.plusSeconds(3))).isFalse();
  }

  @Test
  void lostWorkflowPreservesCheckoutAndAssetState() {
    EquipmentAsset a = asset();
    a.checkout();
    EquipmentCheckout c =
        new EquipmentCheckout(
            org,
            a,
            null,
            "External",
            null,
            Instant.now(),
            Instant.now().plusSeconds(60),
            EquipmentCondition.GOOD,
            null,
            user);
    c.markLost();
    a.markLost();
    assertThat(c.getStatus()).isEqualTo(CheckoutStatus.LOST);
    assertThat(a.getStatus()).isEqualTo(AssetStatus.LOST);
    assertThatThrownBy(c::markLost).isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void maintenanceLifecycleIsExplicit() {
    EquipmentMaintenanceRecord r =
        new EquipmentMaintenanceRecord(
            org, asset(), MaintenanceType.REPAIR, "Repair", Instant.now(), null, null, user, null);
    r.start();
    assertThat(r.getStatus()).isEqualTo(MaintenanceStatus.IN_PROGRESS);
    r.complete(Instant.now(), user);
    assertThat(r.getStatus()).isEqualTo(MaintenanceStatus.COMPLETED);
    assertThatThrownBy(r::cancel).isInstanceOf(BusinessRuleException.class);
  }
}
