import { PageResponse } from "./api.models";
export type FoodCategory =
  | "CANNED_GOODS"
  | "GRAINS"
  | "PRODUCE"
  | "DAIRY"
  | "MEAT"
  | "FROZEN"
  | "BAKERY"
  | "BEVERAGES"
  | "BABY_FOOD"
  | "PERSONAL_CARE"
  | "HOUSEHOLD"
  | "OTHER";
export type UnitType =
  | "EACH"
  | "CAN"
  | "BOX"
  | "BAG"
  | "BOTTLE"
  | "POUND"
  | "OUNCE"
  | "KILOGRAM"
  | "LITER"
  | "PACKAGE"
  | "OTHER";
export type InventorySourceType =
  | "DONATION"
  | "PURCHASE"
  | "FOOD_BANK"
  | "GOVERNMENT_PROGRAM"
  | "TRANSFER"
  | "OTHER";
export type InventoryAdjustmentType =
  "COUNT_INCREASE" | "COUNT_DECREASE" | "SPOILAGE" | "EXPIRATION";
export type DistributionVisitStatus = "OPEN" | "COMPLETED" | "CANCELLED";
export interface PantryLocation {
  id: string;
  organizationId: string;
  name: string;
  description: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  city: string | null;
  state: string | null;
  postalCode: string | null;
  country: string | null;
  active: boolean;
  timezone: string;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}
export interface PantryItem {
  id: string;
  organizationId: string;
  sku: string | null;
  name: string;
  description: string | null;
  category: FoodCategory;
  unitType: UnitType;
  active: boolean;
  trackExpiration: boolean;
  reorderThreshold: number | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}
export interface InventoryLot {
  id: string;
  pantryId: string;
  pantryName: string;
  itemId: string;
  itemName: string;
  lotNumber: string | null;
  receivedDate: string;
  expirationDate: string | null;
  quantityReceived: number;
  quantityRemaining: number;
  sourceType: InventorySourceType | null;
  sourceReference: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}
export interface InventoryAvailability {
  pantryId: string;
  itemId: string;
  itemName: string;
  category: FoodCategory;
  unitType: UnitType;
  availableQuantity: number;
  expiringSoonQuantity: number;
  expiredQuantity: number;
  reorderThreshold: number;
  lowStock: boolean;
}
export interface InventoryTransaction {
  id: string;
  lotId: string;
  transactionType: string;
  quantity: number;
  occurredAt: string;
  referenceType: string | null;
  referenceId: string | null;
  notes: string | null;
  createdByUserId: string;
}
export interface HouseholdSummary {
  id: string;
  externalReferenceNumber: string | null;
  displayName: string;
  householdSize: number;
  active: boolean;
  createdAt: string;
}
export interface HouseholdDetail {
  id: string;
  organizationId: string;
  externalReferenceNumber: string | null;
  householdName: string | null;
  primaryContactFirstName: string | null;
  primaryContactLastName: string | null;
  email: string | null;
  phone: string | null;
  address: string | null;
  householdSize: number;
  active: boolean;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}
export interface DistributionItem {
  id: string;
  itemId: string;
  itemName: string;
  quantity: number;
}
export interface DistributionSummary {
  id: string;
  pantryId: string;
  householdId: string | null;
  recipientDisplayName: string;
  visitDateTime: string;
  householdSizeAtVisit: number;
  status: DistributionVisitStatus;
  completedAt: string | null;
}
export interface DistributionDetail extends DistributionSummary {
  organizationId: string;
  pantryName: string;
  notes: string | null;
  servedByUserId: string;
  cancelledAt: string | null;
  items: DistributionItem[];
  createdAt: string;
  updatedAt: string;
  version: number;
}
export interface PantryInventorySummary {
  organizationId: string;
  pantryId: string;
  totalItemTypes: number;
  totalAvailableQuantity: number;
  lowStockItemCount: number;
  expiredLotCount: number;
  expiringSoonLotCount: number;
}
export interface PantryDistributionReport {
  organizationId: string;
  pantryId: string;
  from: string;
  to: string;
  visitsCompleted: number;
  householdsServed: number;
  uniqueHouseholdsServed: number;
  totalQuantityDistributed: number;
  averageHouseholdSize: number;
  quantityByCategory: { category: FoodCategory; quantity: number }[];
}
export interface PantryHouseholdReport {
  organizationId: string;
  from: string;
  to: string;
  activeHouseholds: number;
  householdVisits: number;
  householdsServedOnce: number;
  householdsServedMultipleTimes: number;
}
export interface PantryWasteReport {
  organizationId: string;
  pantryId: string;
  from: string;
  to: string;
  expiredQuantity: number;
  spoiledQuantity: number;
}
export type PantryPage<T> = PageResponse<T>;
