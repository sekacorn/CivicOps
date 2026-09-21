export type AssetStatus =
  "AVAILABLE" | "CHECKED_OUT" | "MAINTENANCE" | "LOST" | "RETIRED";
export type EquipmentCondition =
  "EXCELLENT" | "GOOD" | "FAIR" | "DAMAGED" | "UNUSABLE";
export type CheckoutStatus = "ACTIVE" | "RETURNED" | "LOST" | "CANCELLED";
export type MaintenanceStatus =
  "OPEN" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";
export type MaintenanceType =
  "INSPECTION" | "REPAIR" | "CLEANING" | "CALIBRATION" | "UPGRADE" | "OTHER";

export interface EquipmentCategory {
  id: string;
  name: string;
  description: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface EquipmentAssetSummary {
  id: string;
  assetTag: string;
  name: string;
  categoryId: string | null;
  categoryName: string | null;
  condition: EquipmentCondition;
  status: AssetStatus;
  location: string | null;
}

export interface EquipmentAssetDetail extends EquipmentAssetSummary {
  organizationId: string;
  description: string | null;
  manufacturer: string | null;
  model: string | null;
  serialNumber: string | null;
  purchaseDate: string | null;
  purchaseValue: number | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface EquipmentCheckoutSummary {
  id: string;
  assetId: string;
  assetTag: string;
  assetName: string;
  status: CheckoutStatus;
  checkedOutAt: string;
  dueAt: string;
  checkedInAt: string | null;
  overdue: boolean;
}

export interface EquipmentCheckoutDetail extends EquipmentCheckoutSummary {
  organizationId: string;
  borrowerUserId: string | null;
  borrowerName: string;
  borrowerEmail: string | null;
  checkoutCondition: EquipmentCondition;
  returnCondition: EquipmentCondition | null;
  notes: string | null;
  issuedByUserId: string;
  receivedByUserId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface EquipmentMaintenance {
  id: string;
  assetId: string;
  assetTag: string;
  maintenanceType: MaintenanceType;
  description: string;
  startedAt: string;
  completedAt: string | null;
  cost: number | null;
  vendor: string | null;
  status: MaintenanceStatus;
  createdByUserId: string;
  completedByUserId: string | null;
  notes: string | null;
  createdAt: string;
}

export interface EquipmentInventoryReport {
  organizationId: string;
  totalAssets: number;
  availableAssets: number;
  checkedOutAssets: number;
  maintenanceAssets: number;
  lostAssets: number;
  retiredAssets: number;
  overdueCheckouts: number;
}

export interface EquipmentUtilizationReport {
  organizationId: string;
  from: string;
  to: string;
  checkoutCount: number;
  mostFrequentlyCheckedOut: Array<{
    assetId: string;
    assetTag: string;
    assetName: string;
    checkoutCount: number;
  }>;
  currentlyOverdue: number;
  currentlyInMaintenance: number;
}

export interface EquipmentMaintenanceReport {
  organizationId: string;
  from: string;
  to: string;
  openMaintenance: number;
  recordsByType: Partial<Record<MaintenanceType, number>>;
  totalCost: number;
}
