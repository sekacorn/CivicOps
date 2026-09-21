import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { API_BASE_URL } from "../../core/http/api-url";
import { PageResponse } from "../../core/models/api.models";
import {
  DistributionDetail,
  DistributionSummary,
  HouseholdDetail,
  HouseholdSummary,
  PantryDistributionReport,
  PantryHouseholdReport,
  PantryInventorySummary,
  InventoryAvailability,
  InventoryLot,
  InventoryTransaction,
  PantryItem,
  PantryLocation,
  PantryWasteReport,
} from "../../core/models/food-pantry.models";

@Injectable({ providedIn: "root" })
export class FoodPantryApiService {
  private readonly http = inject(HttpClient);
  private readonly base = inject(API_BASE_URL);

  private org(org: string): string {
    return `${this.base}/organizations/${org}`;
  }

  pantries(org: string, page = 0) {
    return this.http.get<PageResponse<PantryLocation>>(
      `${this.org(org)}/food-pantries`,
      { params: { page, size: 20, sort: "name,asc" } },
    );
  }

  pantry(org: string, pantryId: string) {
    return this.http.get<PantryLocation>(
      `${this.org(org)}/food-pantries/${pantryId}`,
    );
  }

  createPantry(org: string, value: Record<string, unknown>) {
    return this.http.post<PantryLocation>(
      `${this.org(org)}/food-pantries`,
      value,
    );
  }

  updatePantry(org: string, pantryId: string, value: Record<string, unknown>) {
    return this.http.patch<PantryLocation>(
      `${this.org(org)}/food-pantries/${pantryId}`,
      value,
    );
  }

  pantryAction(
    org: string,
    pantryId: string,
    action: "activate" | "deactivate",
  ) {
    return this.http.post<PantryLocation>(
      `${this.org(org)}/food-pantries/${pantryId}/${action}`,
      {},
    );
  }

  items(org: string, page = 0) {
    return this.http.get<PageResponse<PantryItem>>(
      `${this.org(org)}/pantry-items`,
      {
        params: { page, size: 50, sort: "name,asc" },
      },
    );
  }

  item(org: string, itemId: string) {
    return this.http.get<PantryItem>(`${this.org(org)}/pantry-items/${itemId}`);
  }

  createItem(org: string, value: Record<string, unknown>) {
    return this.http.post<PantryItem>(`${this.org(org)}/pantry-items`, value);
  }

  updateItem(org: string, itemId: string, value: Record<string, unknown>) {
    return this.http.patch<PantryItem>(
      `${this.org(org)}/pantry-items/${itemId}`,
      value,
    );
  }

  itemAction(org: string, itemId: string, action: "activate" | "deactivate") {
    return this.http.post<PantryItem>(
      `${this.org(org)}/pantry-items/${itemId}/${action}`,
      {},
    );
  }

  receipt(org: string, pantryId: string, value: Record<string, unknown>) {
    return this.http.post<InventoryLot>(
      `${this.org(org)}/food-pantries/${pantryId}/inventory-receipts`,
      value,
    );
  }

  inventory(org: string, pantryId: string, page = 0) {
    return this.http.get<PageResponse<InventoryLot>>(
      `${this.org(org)}/food-pantries/${pantryId}/inventory`,
      { params: { page, size: 20, sort: "expirationDate,asc" } },
    );
  }

  availability(org: string, pantryId: string) {
    return this.http.get<InventoryAvailability[]>(
      `${this.org(org)}/food-pantries/${pantryId}/inventory/availability`,
    );
  }

  inventoryLot(org: string, lotId: string) {
    return this.http.get<InventoryLot>(
      `${this.org(org)}/pantry-inventory/${lotId}`,
    );
  }

  adjustLot(org: string, lotId: string, value: Record<string, unknown>) {
    return this.http.post<InventoryLot>(
      `${this.org(org)}/pantry-inventory/${lotId}/adjust`,
      value,
    );
  }

  transactions(org: string, lotId: string) {
    return this.http.get<PageResponse<InventoryTransaction>>(
      `${this.org(org)}/pantry-inventory/${lotId}/transactions`,
      { params: { page: 0, size: 50, sort: "createdAt,desc" } },
    );
  }

  households(org: string, page = 0) {
    return this.http.get<PageResponse<HouseholdSummary>>(
      `${this.org(org)}/pantry-households`,
      { params: { page, size: 20, sort: "householdName,asc" } },
    );
  }

  household(org: string, householdId: string) {
    return this.http.get<HouseholdDetail>(
      `${this.org(org)}/pantry-households/${householdId}`,
    );
  }

  createHousehold(org: string, value: Record<string, unknown>) {
    return this.http.post<HouseholdDetail>(
      `${this.org(org)}/pantry-households`,
      value,
    );
  }

  updateHousehold(
    org: string,
    householdId: string,
    value: Record<string, unknown>,
  ) {
    return this.http.patch<HouseholdDetail>(
      `${this.org(org)}/pantry-households/${householdId}`,
      value,
    );
  }

  deactivateHousehold(org: string, householdId: string) {
    return this.http.post<HouseholdDetail>(
      `${this.org(org)}/pantry-households/${householdId}/deactivate`,
      {},
    );
  }

  householdVisits(org: string, householdId: string) {
    return this.http.get<PageResponse<DistributionSummary>>(
      `${this.org(org)}/pantry-households/${householdId}/visits`,
      { params: { page: 0, size: 50, sort: "visitDateTime,desc" } },
    );
  }

  distributions(org: string, pantryId: string, page = 0) {
    return this.http.get<PageResponse<DistributionSummary>>(
      `${this.org(org)}/food-pantries/${pantryId}/distribution-visits`,
      { params: { page, size: 20, sort: "visitDateTime,desc" } },
    );
  }

  createDistribution(
    org: string,
    pantryId: string,
    value: Record<string, unknown>,
  ) {
    return this.http.post<DistributionDetail>(
      `${this.org(org)}/food-pantries/${pantryId}/distribution-visits`,
      value,
    );
  }

  distribution(org: string, distributionId: string) {
    return this.http.get<DistributionDetail>(
      `${this.org(org)}/pantry-distribution-visits/${distributionId}`,
    );
  }

  addDistributionItem(
    org: string,
    distributionId: string,
    value: Record<string, unknown>,
  ) {
    return this.http.post<DistributionDetail>(
      `${this.org(org)}/pantry-distribution-visits/${distributionId}/items`,
      value,
    );
  }

  distributionAction(
    org: string,
    distributionId: string,
    action: "complete" | "cancel",
  ) {
    return this.http.post<DistributionDetail>(
      `${this.org(org)}/pantry-distribution-visits/${distributionId}/${action}`,
      {},
    );
  }

  inventoryReport(org: string, pantryId?: string) {
    let params = new HttpParams();
    if (pantryId) params = params.set("pantryId", pantryId);
    return this.http.get<PantryInventorySummary>(
      `${this.org(org)}/food-pantry-reports/inventory-summary`,
      { params },
    );
  }

  distributionReport(
    org: string,
    pantryId?: string,
    from?: string,
    to?: string,
  ) {
    let params = new HttpParams();
    if (pantryId) params = params.set("pantryId", pantryId);
    if (from) params = params.set("from", from);
    if (to) params = params.set("to", to);
    return this.http.get<PantryDistributionReport>(
      `${this.org(org)}/food-pantry-reports/distributions`,
      { params },
    );
  }

  householdReport(org: string) {
    return this.http.get<PantryHouseholdReport>(
      `${this.org(org)}/food-pantry-reports/households`,
    );
  }

  wasteReport(org: string, pantryId?: string) {
    let params = new HttpParams();
    if (pantryId) params = params.set("pantryId", pantryId);
    return this.http.get<PantryWasteReport>(
      `${this.org(org)}/food-pantry-reports/waste`,
      { params },
    );
  }
}
