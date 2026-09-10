package org.civicops.foodpantry.inventory;

public enum InventoryTransactionType {
  RECEIPT,
  DISTRIBUTION,
  ADJUSTMENT_INCREASE,
  ADJUSTMENT_DECREASE,
  EXPIRATION,
  SPOILAGE,
  TRANSFER_OUT,
  TRANSFER_IN
}
