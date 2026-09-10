package org.civicops.equipment.asset;

public enum EquipmentCondition {
  EXCELLENT,
  GOOD,
  FAIR,
  DAMAGED,
  UNUSABLE;

  public boolean usable() {
    return this == EXCELLENT || this == GOOD || this == FAIR;
  }
}
